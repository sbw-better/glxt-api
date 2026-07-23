package com.citics.glxtapi.plugin.sql;

import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.vo.ProcedureExecuteResult;
import com.citics.glxtapi.web.service.TenantService;
import com.citics.glxtapi.web.support.SqlContextHolder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_VARCHAR;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_IN;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_OUT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DbModuleProcedureTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TenantService tenantService;
    @Mock
    private Connection connection;
    @Mock
    private CallableStatement callableStatement;

    @After
    public void tearDown() {
        SqlContextHolder.clear();
        SqlContextHolder.clearCount();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void callProcedureBindsNullInputWithSqlTypeForOracle() throws Exception {
        DbModule dbModule = new DbModule(jdbcTemplate, null, tenantService);
        when(connection.prepareCall("{ call PKG.PROC(?, ?) }")).thenReturn(callableStatement);
        when(callableStatement.getObject(2)).thenReturn("OK");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(invocation -> {
            ConnectionCallback<ProcedureExecuteResult> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        });

        Map<String, Object> params = new HashMap<>();
        params.put("fundCode", null);
        ProcedureExecuteResult result = dbModule.callProcedure("PKG.PROC", Arrays.asList(
                procedureParam("fundCode", PROCEDURE_PARAM_DIRECTION_IN, 1),
                procedureParam("status", PROCEDURE_PARAM_DIRECTION_OUT, 2)
        ), params, true, null);

        verify(callableStatement).setNull(1, Types.VARCHAR);
        verify(callableStatement, never()).setObject(1, null);
        verify(callableStatement).registerOutParameter(2, Types.VARCHAR);
        assertEquals("OK", result.getOutParams().get("status"));
        assertEquals("{ call PKG.PROC(?, ?) }", SqlContextHolder.getSql());
        assertEquals(Integer.valueOf(0), SqlContextHolder.getSqlCount());
    }

    private ApiParam procedureParam(String code, Integer direction, Integer orderNo) {
        ApiParam param = new ApiParam();
        param.setName(code);
        param.setCode(code);
        param.setDirection(direction);
        param.setJdbcType(PROCEDURE_JDBC_TYPE_VARCHAR);
        param.setOrderNo(orderNo);
        return param;
    }
}
