package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.web.entity.ApiParam;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_VALUE_VARCHAR2;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_IN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Oracle integration check; opt in with -Doracle.integration=true.
 * Creates uniquely named probe objects in the configured schema and removes them afterward.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProcedureTransactionOracleIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ProcedureTransactionService procedureTransactionService;

    @Test
    public void procedureFailureRollsBackItsInsert() {
        Assume.assumeTrue("Set -Doracle.integration=true to run against the configured Oracle schema",
                Boolean.getBoolean("oracle.integration"));

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String tableName = "CODEX_TX_" + suffix;
        String procedureName = "CODEX_TXP_" + suffix;
        String rowId = UUID.randomUUID().toString();
        try {
            jdbcTemplate.execute("CREATE TABLE " + tableName + " (ID VARCHAR2(36) PRIMARY KEY)");
            jdbcTemplate.execute("CREATE PROCEDURE " + procedureName + " (P_ID IN VARCHAR2) AS BEGIN "
                    + "INSERT INTO " + tableName + " (ID) VALUES (P_ID); "
                    + "RAISE_APPLICATION_ERROR(-20001, 'codex rollback probe'); END;");

            ApiParam input = new ApiParam();
            input.setCode("p_id");
            input.setDirection(PROCEDURE_PARAM_DIRECTION_IN);
            input.setJdbcType(PROCEDURE_JDBC_TYPE_VALUE_VARCHAR2);
            input.setOrderNo(1);
            Map<String, Object> values = new HashMap<>();
            values.put("p_id", rowId);

            try {
                procedureTransactionService.execute(procedureName, Collections.singletonList(input), values, null);
                fail("Expected the procedure to raise an exception");
            } catch (RuntimeException expected) {
                // The exception must escape the @Transactional proxy to trigger rollback.
            }

            Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE ID = ?",
                    Integer.class, rowId);
            assertEquals("The procedure's insert should have been rolled back", Integer.valueOf(0), rows);
        } finally {
            jdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE " + procedureName
                    + "'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -4043 THEN RAISE; END IF; END;");
            jdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE " + tableName
                    + " PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;");
        }
    }
}
