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

    @Test
    @SuppressWarnings("unchecked")
    public void callProcedureBindsByOrderNoWhenConfigurationListIsUnsorted() throws Exception {
        DbModule dbModule = new DbModule(jdbcTemplate, null, tenantService);
        when(connection.prepareCall("{ call PKG.PROC(?, ?) }")).thenReturn(callableStatement);
        when(callableStatement.getObject(2)).thenReturn("OK");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<ProcedureExecuteResult>) invocation.getArgument(0)).doInConnection(connection));

        Map<String, Object> params = new HashMap<>();
        params.put("fundCode", "A001");
        ProcedureExecuteResult result = dbModule.callProcedure("PKG.PROC", Arrays.asList(
                procedureParam("status", PROCEDURE_PARAM_DIRECTION_OUT, 2),
                procedureParam("fundCode", PROCEDURE_PARAM_DIRECTION_IN, 1)
        ), params, false, null);

        verify(callableStatement).setObject(1, "A001");
        verify(callableStatement).registerOutParameter(2, Types.VARCHAR);
        assertEquals("OK", result.getOutParams().get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void callProcedureSupportsClobInputOutputInoutAndNull() throws Exception {
        DbModule dbModule = new DbModule(jdbcTemplate, null, tenantService);
        when(connection.prepareCall("{ call PKG.CLOB_PROC(?, ?, ?, ?) }")).thenReturn(callableStatement);
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<ProcedureExecuteResult>) invocation.getArgument(0)).doInConnection(connection));
        String text = String.join("", java.util.Collections.nCopies(40000, "中文A"));
        java.sql.Clob output = org.mockito.Mockito.mock(java.sql.Clob.class);
        java.io.Reader reader = org.mockito.Mockito.mock(java.io.Reader.class);
        java.io.StringReader source = new java.io.StringReader(text);
        when(reader.read(any(char[].class))).thenAnswer(invocation -> source.read((char[]) invocation.getArgument(0)));
        when(output.getCharacterStream()).thenReturn(reader);
        when(callableStatement.getClob(2)).thenReturn(output);
        when(callableStatement.getClob(3)).thenReturn(null);
        ApiParam in = procedureParam("input", 1, 1);
        ApiParam inout = procedureParam("inout", 3, 2);
        ApiParam out = procedureParam("output", 2, 3);
        ApiParam empty = procedureParam("empty", 1, 4);
        for (ApiParam param : Arrays.asList(in, inout, out, empty)) {
            param.setJdbcType("CLOB");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("input", text);
        params.put("inout", text);
        ProcedureExecuteResult result = dbModule.callProcedure("PKG.CLOB_PROC",
                Arrays.asList(in, inout, out, empty), params, false, null);
        org.mockito.ArgumentCaptor<java.io.Reader> inputReader = org.mockito.ArgumentCaptor.forClass(java.io.Reader.class);
        verify(callableStatement).setClob(org.mockito.ArgumentMatchers.eq(1), inputReader.capture(), org.mockito.ArgumentMatchers.eq((long) text.length()));
        StringBuilder bound = new StringBuilder();
        int c;
        while ((c = inputReader.getValue().read()) != -1) { bound.append((char) c); }
        assertEquals(text, bound.toString());
        verify(callableStatement).setClob(org.mockito.ArgumentMatchers.eq(2), any(java.io.Reader.class), org.mockito.ArgumentMatchers.eq((long) text.length()));
        verify(callableStatement).setNull(4, Types.CLOB);
        verify(callableStatement).registerOutParameter(2, Types.CLOB);
        verify(callableStatement).registerOutParameter(3, Types.CLOB);
        assertEquals(text, result.getOutParams().get("inout"));
        org.junit.Assert.assertTrue(result.getOutParams().containsKey("output"));
        org.junit.Assert.assertNull(result.getOutParams().get("output"));
        verify(reader).close();
        verify(output).free();
        verify(callableStatement).close();
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
