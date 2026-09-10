package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import org.junit.Test;

import java.util.Arrays;

import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_CLOB;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_CURSOR;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_DATE;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_DECIMAL;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_BIGINT;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_INTEGER;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_TIMESTAMP;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_VARCHAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiServiceImplProcedureJdbcTypeTest {

    @Test
    public void normalizesFrontendDictionaryValuesToJdbcTypeStrings() {
        assertEquals(PROCEDURE_JDBC_TYPE_VARCHAR, ApiServiceImpl.normalizeProcedureJdbcType("1"));
        assertEquals(PROCEDURE_JDBC_TYPE_INTEGER, ApiServiceImpl.normalizeProcedureJdbcType("2"));
        assertEquals(PROCEDURE_JDBC_TYPE_BIGINT, ApiServiceImpl.normalizeProcedureJdbcType("3"));
        assertEquals(PROCEDURE_JDBC_TYPE_DECIMAL, ApiServiceImpl.normalizeProcedureJdbcType("4"));
        assertEquals(PROCEDURE_JDBC_TYPE_DATE, ApiServiceImpl.normalizeProcedureJdbcType("5"));
        assertEquals(PROCEDURE_JDBC_TYPE_TIMESTAMP, ApiServiceImpl.normalizeProcedureJdbcType("6"));
        assertEquals(PROCEDURE_JDBC_TYPE_CLOB, ApiServiceImpl.normalizeProcedureJdbcType("7"));
        assertEquals(PROCEDURE_JDBC_TYPE_CURSOR, ApiServiceImpl.normalizeProcedureJdbcType("8"));
    }

    @Test
    public void preservesExistingStringConfigurations() {
        assertEquals(PROCEDURE_JDBC_TYPE_VARCHAR, ApiServiceImpl.normalizeProcedureJdbcType(" varchar "));
        assertEquals(PROCEDURE_JDBC_TYPE_CLOB, ApiServiceImpl.normalizeProcedureJdbcType("CLOB"));
        assertNull(ApiServiceImpl.normalizeProcedureJdbcType(null));
    }

    @Test
    public void requiresProcedureOrderNumbersToStartAtOneAndBeContinuous() {
        assertTrue(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(1, 2, 3)), 3));
        assertFalse(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(1, 3)), 2));
        assertFalse(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(0, 1)), 2));
    }

    @Test
    public void previewDisplaysJdbcTypeStringsWhenFrontendSubmitsDictionaryValues() {
        ApiParam input = new ApiParam();
        input.setCode("content");
        input.setDirection(1);
        input.setJdbcType("7");
        input.setOrderNo(1);

        ApiParam output = new ApiParam();
        output.setCode("data");
        output.setDirection(2);
        output.setJdbcType("8");
        output.setOrderNo(2);

        ApiInterfaceDTO dto = new ApiInterfaceDTO();
        dto.setType(2);
        dto.setProcedureName("PKG_EMP.QUERY_EMP");
        dto.setApiParamList(Arrays.asList(input, output));

        assertEquals("{ call PKG_EMP.QUERY_EMP(? /* content:IN:CLOB */, ? /* data:OUT:CURSOR */) }",
                new ApiServiceImpl().preview(dto));
    }
}
