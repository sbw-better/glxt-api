package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.common.utils.file.ToolUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ApiActuatorServiceImplTest {

    private static final String REQUEST_BODY = "{\"apiCode\":\"demoApi\"}";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String originalTempDir = System.getProperty("java.io.tmpdir");

    @After
    public void tearDown() {
        System.setProperty("java.io.tmpdir", originalTempDir);
    }

    @Test
    public void executeExportWritesExcelAndReturnsDownloadFileName() {
        System.setProperty("java.io.tmpdir", temporaryFolder.getRoot().getAbsolutePath());
        ApiActuatorServiceImpl service = spy(new ApiActuatorServiceImpl());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1);
        row.put("name", "demo");
        HttpServletRequest request = mock(HttpServletRequest.class);
        doReturn(Collections.singletonList(row)).when(service).execute(eq(REQUEST_BODY), any(HttpServletRequest.class));

        String fileName = service.executeExport(REQUEST_BODY, request);

        assertTrue(fileName.endsWith(".xlsx"));
        assertTrue(fileName.contains("_demoApi_"));
        assertTrue(new File(ToolUtil.getDownloadPath(), fileName).exists());
    }
}
