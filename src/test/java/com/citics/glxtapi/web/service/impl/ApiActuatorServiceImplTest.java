package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.common.page.PageInfoResult;
import com.citics.glxtapi.common.utils.file.ToolUtil;
import com.citics.glxtapi.plugin.sql.DbModule;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.citics.glxtapi.web.constants.Constants.API_DEFAULT_DATA_SOURCE_ID;
import static com.citics.glxtapi.web.constants.Constants.API_FIELD_BACK_MODE_DEFAULT;
import static com.citics.glxtapi.web.constants.Constants.WHETHER_YES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiActuatorServiceImplTest {

    private static final String QUERY_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"params\":{}}";
    private static final String EXPORT_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"exportExcel\":true,\"params\":{}}";
    private static final String EXPORT_PAGE_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"systemCode\":\"front-system\",\"exportExcel\":true,\"pageNeed\":true,\"pageNum\":1,\"pageSize\":20,\"params\":{}}";

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
    public void executeWritesExcelAndReturnsDownloadFileNameWhenExportExcelIsTrue() {
        when(dbModule.select(anyString(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT)))
                .thenReturn(Collections.singletonList(row("id", 1, "name", "demo")));

        Object result = service.execute(EXPORT_BODY, request);

        String fileName = (String) result;
        assertTrue(fileName.endsWith(".xlsx"));
        assertTrue(fileName.contains("_demoApi_"));
        assertTrue(new File(ToolUtil.getDownloadPath(), fileName).exists());
    }

    @Test
    public void executeWritesExcelFromCurrentPageListWhenPageResultReturned() {
        ApiInterfaceVO pageApi = apiInterface(WHETHER_YES);
        when(apiService.getByApi("demo", "demoApi")).thenReturn(pageApi);
        PageInfoResult pageInfoResult = new PageInfoResult(Arrays.asList(row("code", "A001", "status", null)));
        when(dbModule.page2(anyString(), anyLong(), anyLong(), anyMap(), eq(true), eq(API_FIELD_BACK_MODE_DEFAULT))).thenReturn(pageInfoResult);

        Object result = service.execute(EXPORT_PAGE_BODY, request);

        String fileName = (String) result;
        assertTrue(fileName.endsWith(".xlsx"));
        assertTrue(new File(ToolUtil.getDownloadPath(), fileName).exists());
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

    private Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
