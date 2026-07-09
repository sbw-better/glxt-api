package com.citics.glxtapi.common.utils.excel;

import com.citics.glxtapi.common.page.PageInfoResult;
import com.citics.glxtapi.common.utils.file.ToolUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExcelExportUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String originalTempDir = System.getProperty("java.io.tmpdir");

    @After
    public void tearDown() {
        System.setProperty("java.io.tmpdir", originalTempDir);
    }

    @Test
    public void exportNameWritesDynamicRowsToDownloadDirectory() throws Exception {
        System.setProperty("java.io.tmpdir", temporaryFolder.getRoot().getAbsolutePath());
        List<Map<String, Object>> rows = Arrays.asList(row("id", 1, "name", "alpha"), row("id", 2, "amount", new BigDecimal("12.50")));

        String fileName = ExcelExportUtils.exportName(rows, "execute_result");

        assertTrue(fileName.endsWith("_execute_result.xlsx"));
        File excelFile = new File(ToolUtil.getDownloadPath(), fileName);
        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);

        Workbook workbook = WorkbookFactory.create(new FileInputStream(excelFile));
        try {
            Sheet sheet = workbook.getSheet("result");
            assertNotNull(sheet);
            Row header = sheet.getRow(0);
            assertEquals("id", header.getCell(0).getStringCellValue());
            assertEquals("name", header.getCell(1).getStringCellValue());
            assertEquals("amount", header.getCell(2).getStringCellValue());
            assertEquals(1D, sheet.getRow(1).getCell(0).getNumericCellValue(), 0.0001D);
            assertEquals("alpha", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(12.50D, sheet.getRow(2).getCell(2).getNumericCellValue(), 0.0001D);
        } finally {
            workbook.close();
        }
    }

    @Test
    public void exportNameUsesPageInfoList() throws Exception {
        System.setProperty("java.io.tmpdir", temporaryFolder.getRoot().getAbsolutePath());
        PageInfoResult pageInfoResult = new PageInfoResult(Arrays.asList(row("code", "A001", "status", null)));

        String fileName = ExcelExportUtils.exportName(pageInfoResult, "page_result");

        File excelFile = new File(ToolUtil.getDownloadPath(), fileName);
        Workbook workbook = WorkbookFactory.create(new FileInputStream(excelFile));
        try {
            Sheet sheet = workbook.getSheet("result");
            assertEquals("code", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("status", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("A001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("", sheet.getRow(1).getCell(1).getStringCellValue());
        } finally {
            workbook.close();
        }
    }

    private Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
