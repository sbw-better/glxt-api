package com.citics.glxtapi.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.citics.glxtapi.common.page.PageInfoResult;
import com.citics.glxtapi.plugin.sql.DbModule;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.vo.ApiActuatorExcelResult;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.ProcedureExecuteResult;
import com.citics.glxtapi.web.service.ApiParamService;
import com.citics.glxtapi.web.service.ApiService;
import com.citics.glxtapi.web.service.ConnectionService;
import com.citics.glxtapi.web.service.TenantAuthService;
import com.citics.glxtapi.web.service.TenantService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.citics.glxtapi.web.constants.Constants.API_DEFAULT_DATA_SOURCE_ID;
import static com.citics.glxtapi.web.constants.Constants.API_FIELD_BACK_MODE_DEFAULT;
import static com.citics.glxtapi.web.constants.Constants.FILED_CHECK_TYPE_NO;
import static com.citics.glxtapi.web.constants.Constants.INTERFACE_TYPE_PROCEDURE;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_CURSOR;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_JDBC_TYPE_VARCHAR;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_IN;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_OUT;
import static com.citics.glxtapi.web.constants.Constants.WHETHER_NO;
import static com.citics.glxtapi.web.constants.Constants.WHETHER_YES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiActuatorServiceImplTest {

    private static final String QUERY_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"params\":{}}";
    private static final String EXPORT_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"exportExcel\":true,\"params\":{}}";
    private static final String EXPORT_PAGE_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"exportExcel\":true,\"pageNeed\":true,\"pageNum\":1,\"pageSize\":20,\"params\":{}}";
    private static final String PROCEDURE_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"params\":{\"fundCode\":\"A001\"}}";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ApiService apiService;
    @Mock
    private TenantService tenantService;
    @Mock
    private ApiParamService apiParamService;
    @Mock
    private TenantAuthService tenantAuthService;
    @Mock
    private ConnectionService connectionService;
    @Mock
    private DbModule dbModule;
    @Mock
    private HttpServletRequest request;

    private ApiActuatorServiceImpl service;
    private final String originalTempDir = System.getProperty("java.io.tmpdir");

    @Before
    public void setUp() {
        System.setProperty("java.io.tmpdir", temporaryFolder.getRoot().getAbsolutePath());
        service = new ApiActuatorServiceImpl();
        ReflectionTestUtils.setField(service, "apiService", apiService);
        ReflectionTestUtils.setField(service, "tenantService", tenantService);
        ReflectionTestUtils.setField(service, "apiParamService", apiParamService);
        ReflectionTestUtils.setField(service, "tenantAuthService", tenantAuthService);
        ReflectionTestUtils.setField(service, "connectionService", connectionService);
        ReflectionTestUtils.setField(service, "dbModule", dbModule);
        ReflectionTestUtils.setField(service, "sqlResultPageMaxRow", 5000);

        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(apiService.getByApi("demo", "demoApi")).thenReturn(apiInterface(0));
        when(tenantAuthService.hasExecutePermission(eq(1L), eq("token"), eq("10.0.0.1"))).thenReturn(true);
        when(apiParamService.apiHaveRequiredParam(1L)).thenReturn(false);
        when(apiParamService.requiredParamIsFull(eq(1L), anyList())).thenReturn(true);
        when(apiParamService.notRequiredParamList(eq(1L), anyList())).thenReturn(Collections.emptyList());
        when(apiParamService.listByCodeList(eq(1L), anyList())).thenReturn(Collections.emptyList());
    }

    @After
    public void tearDown() {
        System.setProperty("java.io.tmpdir", originalTempDir);
    }

    @Test
    public void executeReturnsOriginalQueryResultWhenExportExcelIsMissing() {
        List<Map<String, Object>> rows = Collections.singletonList(row("id", 1, "name", "demo"));
        when(dbModule.select(anyString(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT))).thenReturn(rows);

        Object result = service.execute(QUERY_BODY, request);

        assertSame(rows, result);
        assertTrue(!new File(temporaryFolder.getRoot(), "download").exists());
    }

    @Test
    public void executeExcelReturnsWorkbookBytesAndFileName() throws Exception {
        when(dbModule.select(anyString(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT)))
                .thenReturn(Collections.singletonList(row("id", 1, "name", "demo")));

        ApiActuatorExcelResult result = service.executeExcel(EXPORT_BODY, request);

        assertTrue(result.getFileName().endsWith(".xlsx"));
        assertTrue(result.getFileName().startsWith("demoApi_"));
        assertTrue(result.getContent().length > 0);
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.getContent()));
        try {
            Sheet sheet = workbook.getSheet("result");
            assertEquals("id", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("name", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals(1D, sheet.getRow(1).getCell(0).getNumericCellValue(), 0.0001D);
            assertEquals("demo", sheet.getRow(1).getCell(1).getStringCellValue());
        } finally {
            workbook.close();
        }
        assertTrue(!new File(temporaryFolder.getRoot(), "download").exists());
    }

    @Test
    public void executeExcelWritesCurrentPageListWhenPageResultReturned() throws Exception {
        ApiInterfaceVO pageApi = apiInterface(WHETHER_YES);
        when(apiService.getByApi("demo", "demoApi")).thenReturn(pageApi);
        PageInfoResult pageInfoResult = new PageInfoResult(Arrays.asList(row("code", "A001", "status", null)));
        when(dbModule.page2(anyString(), anyLong(), anyLong(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT))).thenReturn(pageInfoResult);

        ApiActuatorExcelResult result = service.executeExcel(EXPORT_PAGE_BODY, request);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.getContent()));
        try {
            Sheet sheet = workbook.getSheet("result");
            assertEquals("code", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("status", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("A001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("", sheet.getRow(1).getCell(1).getStringCellValue());
        } finally {
            workbook.close();
        }
        assertTrue(!new File(temporaryFolder.getRoot(), "download").exists());
    }

    @Test
    public void executeReturnsOriginalQueryResultWhenExportExcelIsFalse() {
        String requestBody = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"exportExcel\":false,\"params\":{}}";
        List<Map<String, Object>> rows = Collections.singletonList(row("id", 2));
        when(dbModule.select(anyString(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT))).thenReturn(rows);

        Object result = service.execute(requestBody, request);

        assertEquals(rows, result);
        assertTrue(!new File(temporaryFolder.getRoot(), "download").exists());
    }

    @Test
    public void executeProcedureCallsProcedureBranchAndReturnsProcedureResult() {
        ApiInterfaceVO procedureApi = procedureApi();
        ProcedureExecuteResult procedureResult = new ProcedureExecuteResult();
        procedureResult.getOutParams().put("status", "0");
        procedureResult.getCursors().put("data", Collections.singletonList(row("code", "A001")));
        procedureResult.setResultCount(1);
        when(apiService.getByApi("demo", "demoApi")).thenReturn(procedureApi);
        when(dbModule.callProcedure(eq("PKG.PROC"), anyList(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT)))
                .thenReturn(procedureResult);

        Object result = service.execute(PROCEDURE_BODY, request);

        assertSame(procedureResult, result);
        verify(dbModule).callProcedure(eq("PKG.PROC"), anyList(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT));
        verify(dbModule, never()).select(anyString(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT));
    }

    @Test
    public void executeExcelExportsSingleProcedureCursor() throws Exception {
        ApiInterfaceVO procedureApi = procedureApi();
        ProcedureExecuteResult procedureResult = new ProcedureExecuteResult();
        procedureResult.getCursors().put("data", Collections.singletonList(row("code", "A001", "name", "demo")));
        procedureResult.setResultCount(1);
        when(apiService.getByApi("demo", "demoApi")).thenReturn(procedureApi);
        when(dbModule.callProcedure(eq("PKG.PROC"), anyList(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT)))
                .thenReturn(procedureResult);

        ApiActuatorExcelResult result = service.executeExcel(PROCEDURE_BODY, request);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.getContent()));
        try {
            Sheet sheet = workbook.getSheet("result");
            assertEquals("code", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("name", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("A001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("demo", sheet.getRow(1).getCell(1).getStringCellValue());
        } finally {
            workbook.close();
        }
    }

    @Test
    public void procedureInfoCheckAllowsOptionalNullInputForOracleSetNullBinding() {
        ApiInterfaceVO procedureApi = procedureApi();
        ApiParam inputParam = procedureApi.getApiParamList().get(0);
        inputParam.setRequired(WHETHER_NO);
        JSONObject body = JSON.parseObject("{\"token\":\"token\",\"systemCode\":\"front-system\",\"params\":{\"fundCode\":null}}");

        Map<String, Object> params = service.procedureInfoCheck(procedureApi, body, "10.0.0.1");

        assertTrue(params.containsKey("fundCode"));
        assertNull(params.get("fundCode"));
    }

    @Test(expected = MybatisPlusException.class)
    public void procedureInfoCheckRejectsRequiredNullInput() {
        ApiInterfaceVO procedureApi = procedureApi();
        JSONObject body = JSON.parseObject("{\"token\":\"token\",\"systemCode\":\"front-system\",\"params\":{\"fundCode\":null}}");

        service.procedureInfoCheck(procedureApi, body, "10.0.0.1");
    }

    private ApiInterfaceVO apiInterface(Integer page) {
        ApiInterfaceVO apiInterface = new ApiInterfaceVO();
        apiInterface.setId(1L);
        apiInterface.setCode("demoApi");
        apiInterface.setPage(page);
        apiInterface.setConnectionId(API_DEFAULT_DATA_SOURCE_ID);
        apiInterface.setFieldBackMode(API_FIELD_BACK_MODE_DEFAULT);
        apiInterface.setSelectParam("*");
        apiInterface.setFromParam("DUAL");
        return apiInterface;
    }

    private ApiInterfaceVO procedureApi() {
        ApiInterfaceVO apiInterface = apiInterface(0);
        apiInterface.setType(INTERFACE_TYPE_PROCEDURE);
        apiInterface.setProcedureName("PKG.PROC");
        apiInterface.setApiParamList(Arrays.asList(
                procedureParam("fundCode", PROCEDURE_PARAM_DIRECTION_IN, PROCEDURE_JDBC_TYPE_VARCHAR, 1),
                procedureParam("status", PROCEDURE_PARAM_DIRECTION_OUT, PROCEDURE_JDBC_TYPE_VARCHAR, 2),
                procedureParam("data", PROCEDURE_PARAM_DIRECTION_OUT, PROCEDURE_JDBC_TYPE_CURSOR, 3)
        ));
        return apiInterface;
    }

    private ApiParam procedureParam(String code, Integer direction, String jdbcType, Integer orderNo) {
        ApiParam param = new ApiParam();
        param.setName(code);
        param.setCode(code);
        param.setDirection(direction);
        param.setJdbcType(jdbcType);
        param.setOrderNo(orderNo);
        param.setRequired(direction != null && direction == PROCEDURE_PARAM_DIRECTION_IN ? WHETHER_YES : null);
        param.setValidateType(direction != null && direction == PROCEDURE_PARAM_DIRECTION_IN ? FILED_CHECK_TYPE_NO : null);
        return param;
    }

    private Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
