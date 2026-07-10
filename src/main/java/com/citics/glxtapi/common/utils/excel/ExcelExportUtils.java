package com.citics.glxtapi.common.utils.excel;

import com.citics.glxtapi.common.page.PageInfoResult;
import com.citics.glxtapi.common.utils.file.ToolUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SQL执行结果动态导出工具。
 * <p>
 * SQL返回字段不固定，所以这里按Map的key动态生成表头，并把文件写入通用下载目录。
 * </p>
 */
public class ExcelExportUtils {

    private static final String DEFAULT_SHEET_NAME = "result";

    private ExcelExportUtils() {
    }

    public static String exportName(Object result, String workbookName) {
        String fileName = UUID.randomUUID() + "_" + workbookName + ".xlsx";
        export(result, DEFAULT_SHEET_NAME, fileName);
        return fileName;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractRows(Object result) {
        if (result == null) {
            return new ArrayList<>();
        }
        if (result instanceof PageInfoResult) {
            // 分页导出只导出当前页list，不在这里做全量翻页查询。
            List<Map<String, Object>> list = ((PageInfoResult) result).getList();
            return list == null ? new ArrayList<>() : list;
        }
        if (result instanceof List) {
            return (List<Map<String, Object>>) result;
        }
        throw new IllegalArgumentException("SQL执行结果不支持导出Excel");
    }

    private static void export(Object result, String sheetName, String fileName) {
        // 使用SXSSFWorkbook降低大结果集导出时的内存占用。
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try {
            Sheet sheet = workbook.createSheet(sheetName);
            List<Map<String, Object>> rows = extractRows(result);
            List<String> headers = collectHeaders(rows);
            writeHeader(sheet, headers);
            writeBody(workbook, sheet, headers, rows);
            resizeColumns(sheet, headers.size());
            writeWorkbook(workbook, fileName);
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static List<String> collectHeaders(List<Map<String, Object>> rows) {
        // 动态SQL每行字段可能不完全一致，按首次出现顺序取所有字段并集作为表头。
        Set<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row != null) {
                headers.addAll(row.keySet());
            }
        }
        return new ArrayList<>(headers);
    }

    private static void writeHeader(Sheet sheet, List<String> headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }
    }

    private static void writeBody(Workbook workbook, Sheet sheet, List<String> headers, List<Map<String, Object>> rows) {
        CreationHelper creationHelper = workbook.getCreationHelper();
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row excelRow = sheet.createRow(rowIndex + 1);
            Map<String, Object> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                Cell cell = excelRow.createCell(columnIndex);
                Object value = row == null ? null : row.get(headers.get(columnIndex));
                writeCell(cell, value, dateStyle);
            }
        }
    }

    private static void writeCell(Cell cell, Object value, CellStyle dateStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Number) {
            cell.setCellValue(toDouble((Number) value));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private static double toDouble(Number value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).doubleValue();
        }
        return value.doubleValue();
    }

    private static void resizeColumns(Sheet sheet, int columnCount) {
        int resizeCount = Math.min(columnCount, 50);
        for (int i = 0; i < resizeCount; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }
    }

    private static void writeWorkbook(Workbook workbook, String fileName) {
        File file = new File(ToolUtil.getDownloadPath() + fileName);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new IllegalStateException("生成Excel文件失败", e);
        }
    }
}
