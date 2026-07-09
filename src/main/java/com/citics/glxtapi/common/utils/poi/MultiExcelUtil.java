package com.citics.glxtapi.common.utils.poi;

import com.citics.glxtapi.common.annotation.Excel;
import com.citics.glxtapi.common.result.ResultModel;
import com.citics.glxtapi.common.utils.date.DateUtils;
import com.citics.glxtapi.common.utils.file.ToolUtil;
import com.citics.glxtapi.common.utils.service.ServiceUtil;
import com.citics.glxtapi.common.utils.string.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 多sheet Excel相关处理
 */
public class MultiExcelUtil<T> {

    private static final Logger log = LoggerFactory.getLogger(MultiExcelUtil.class);

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
     */
    private Excel.Type type;

    /**
     * 工作簿对象
     */
    private Workbook wb;

    /**
     * 工作表对象
     */
    private Sheet sheet;

    /**
     * 工作表序号
     */
    private Integer sheetNo;

    /**
     * 导入导出数据列表
     */
    private List<T> list;

    /**
     * 注解列表
     */
    private List<Field> fields;

    /**
     * 实体对象
     */
    public Class<T> clazz;

    private MultiExcelUtil() {
        createWorkbook();
    }

    public static MultiExcelUtil init() {
        return new MultiExcelUtil();
    }

    public void createSheet(List<T> list, String sheetName, Class<T> clazz) {
        if (list == null) {
            list = new ArrayList<>();
        }
        this.list = list;
        this.sheetName = sheetName;
        this.clazz = clazz;
        this.sheetNo = this.sheetNo == null ? 0 : this.sheetNo + 1;
        createExcelField();
        this.type = Excel.Type.EXPORT;
        fillExcel();
    }

    public List<T> analyzeSheet(MultipartFile file, String sheetName, Class<T> clazz) {
        this.sheetName = sheetName;
        this.clazz = clazz;
        createExcelField();
        this.type = Excel.Type.IMPORT;
        return readExcel(file);
    }

    public String exportName(String workbookName) {
        OutputStream out = null;
        try {
            String filename = encodingFilename(workbookName);
            out = new FileOutputStream(getAbsoluteFile(filename));
            wb.write(out);
            return filename;
        } catch (Exception e) {
            log.error("导出Excel异常{}", e.getMessage());
            return null;
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public ResultModel export(String workbookName) {
        OutputStream out = null;
        try {
            String filename = encodingFilename(workbookName);
            out = new FileOutputStream(getAbsoluteFile(filename));
            wb.write(out);
            return ResultModel.success(filename);
        } catch (Exception e) {
            log.error("导出Excel异常{}", e.getMessage());
            return ResultModel.error("导出Excel失败，请联系网站管理员！");
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public List<T> readExcel(MultipartFile file) {
        List<T> resList = new ArrayList<>();
        try {
            InputStream is = file.getInputStream();
            String filename = file.getOriginalFilename();
            if (filename.endsWith("xlsx")) {
                wb = new XSSFWorkbook(is);
            } else if (filename.endsWith("xls")) {
                wb = new HSSFWorkbook(is);
            }
            Sheet sheet = wb.getSheet(sheetName);
            if (null == sheet) {
                return null;
            }
            // 读第一行表头
            Row rowHead = sheet.getRow(0);
            short cellCount = rowHead.getLastCellNum();
            // 从第二行开始循环
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                T object = this.clazz.newInstance();
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    for (int c = 0; c < cellCount; c++) {
                        for (Field f : fields) {
                            //设置允许通过反射访问私有变量
                            f.setAccessible(true);
                            if (String.valueOf(f.getAnnotation(Excel.class).name()).equals(String.valueOf(rowHead.getCell(c)))) {
                                Cell cell = row.getCell(c);
                                if (cell == null || cell.toString().trim().equals("")) {
                                    f.set(object, null);
                                } else {
                                    Class<?> fieldType = f.getType();
                                    if (fieldType == String.class) {
                                        try {
                                            f.set(object, new DataFormatter().formatCellValue(cell));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e.getMessage());
                                        }
                                    } else if (fieldType == Long.class) {
                                        try {
                                            f.set(object, cell.getNumericCellValue());
                                        } catch (Exception e) {
                                            try {
                                                f.set(object, Long.valueOf(new DataFormatter().formatCellValue(cell)));
                                            } catch (Exception es) {
                                                throw new RuntimeException(e.getMessage() + ";" + es.getMessage());
                                            }
                                        }
                                    } else if (fieldType == Integer.class) {
                                        try {
                                            f.set(object, cell.getNumericCellValue());
                                        } catch (Exception e) {
                                            try {
                                                f.set(object, Integer.valueOf(new DataFormatter().formatCellValue(cell)));
                                            } catch (Exception es) {
                                                throw new RuntimeException(e.getMessage() + ";" + es.getMessage());
                                            }
                                        }
                                    } else if (fieldType == Float.class) {
                                        try {
                                            f.set(object, cell.getNumericCellValue());
                                        } catch (Exception e) {
                                            try {
                                                f.set(object, Float.valueOf(new DataFormatter().formatCellValue(cell)));
                                            } catch (Exception es) {
                                                throw new RuntimeException(e.getMessage() + ";" + es.getMessage());
                                            }
                                        }
                                    } else if (fieldType == Double.class) {
                                        try {
                                            f.set(object, cell.getNumericCellValue());
                                        } catch (Exception e) {
                                            try {
                                                f.set(object, Double.valueOf(new DataFormatter().formatCellValue(cell)));
                                            } catch (Exception es) {
                                                throw new RuntimeException(e.getMessage() + ";" + es.getMessage());
                                            }
                                        }
                                    } else if (fieldType == Date.class) {
                                        try {
                                            f.set(object, cell.getDateCellValue());
                                        } catch (Exception e) {
                                            try {
                                                SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                                f.set(object, ft.parse(new DataFormatter().formatCellValue(cell)));
                                            } catch (Exception es) {
                                                throw new RuntimeException(e.getMessage() + ";" + es.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                resList.add(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return resList;
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @return 结果
     */
    public void fillExcel() {
        this.sheet = wb.createSheet();
        // 设置工作表的名称.
        wb.setSheetName(sheetNo, sheetName);

        Cell cell = null;
        Row row = sheet.createRow(0);

        // 写入各个字段的列头名称
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            Excel attr = field.getAnnotation(Excel.class);
            // 创建列
            cell = row.createCell(i);
            // 设置列中写入内容为String类型
            cell.setCellType(CellType.STRING);
            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            if (attr.name().indexOf("注：") >= 0) {
                Font font = wb.createFont();
                font.setColor(HSSFFont.COLOR_RED);
                cellStyle.setFont(font);
                cellStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.YELLOW.getIndex());
                sheet.setColumnWidth(i, 6000);
            } else {
                Font font = wb.createFont();
                // 粗体显示
                font.setBold(true);
                // 选择需要用到的字体格式
                cellStyle.setFont(font);
                cellStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
                // 设置列宽
                sheet.setColumnWidth(i, (int) ((attr.width() + 0.72) * 256));
                row.setHeight((short) (attr.height() * 20));
            }
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyle.setWrapText(true);
            cell.setCellStyle(cellStyle);

            // 写入列名
            cell.setCellValue(attr.name());

            // 如果设置了提示信息则鼠标放上去提示.
            if (StringUtils.isNotEmpty(attr.prompt())) {
                setXSSFPrompt(sheet, "", attr.prompt(), 1, 100, i, i);
            }
            // 如果设置了combo属性则本列只能选择不能输入
            if (attr.combo().length > 0) {
                setXSSFValidation(sheet, attr.combo(), 1, 100, i, i);
            }
            if (attr.zoom() != 100) {
                sheet.setZoom(attr.zoom());
            }
        }

        if (Excel.Type.EXPORT.equals(type)) {
            fillExcelData(row, cell);
        }
    }

    /**
     * 填充excel数据
     * @param row 单元格行
     * @param cell 类型单元格
     */
    public void fillExcelData(Row row, Cell cell)
    {
        CellStyle cs = wb.createCellStyle();
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        for (int i = 0; i < list.size(); i++)
        {
            row = sheet.createRow(i + 1);
            // 得到导出对象.
            T vo = (T) list.get(i);
            for (int j = 0; j < fields.size(); j++)
            {
                // 获得field.
                Field field = fields.get(j);
                // 设置实体类私有属性可访问
                field.setAccessible(true);
                Excel attr = field.getAnnotation(Excel.class);
                try
                {
                    // 设置行高
                    row.setHeight((short) (attr.height() * 20));
                    // 根据Excel中设置情况决定是否导出,有些情况需要保持为空,希望用户填写这一列.
                    if (attr.isExport())
                    {
                        // 设置内容自动换行
                        if (attr.wrapText())
                        {
                            cs.setWrapText(true);
                        }
                        // 创建cell
                        cell = row.createCell(j);
                        cell.setCellStyle(cs);
                        if (vo == null)
                        {
                            // 如果数据存在就填入,不存在填入空格.
                            cell.setCellValue("");
                            continue;
                        }
                        // 用于读取对象中的属性
                        Object value = getTargetValue(vo, field, attr);
                        String dateFormat = attr.dateFormat();
                        String readConverterExp = attr.readConverterExp();
                        String readDictionaryConv = attr.readDictionaryConv();
                        String readObjConverterExp = attr.readObjConverterExp();
                        if (StringUtils.isNotEmpty(dateFormat) && StringUtils.isNotNull(value))
                        {
                            cell.setCellValue(DateUtils.parseDateToStr(dateFormat, (Date) value));
                        }
                        else if (StringUtils.isNotEmpty(readConverterExp) && StringUtils.isNotNull(value))
                        {
                            cell.setCellValue(convertByExp(String.valueOf(value), readConverterExp));
                        }
                        else if (StringUtils.isNotEmpty(readDictionaryConv) && StringUtils.isNotNull(value))
                        {
                            cell.setCellValue(ServiceUtil.getCommonService().convertByDicExp(String.valueOf(value), readDictionaryConv));
                        }
                        else if (StringUtils.isNotEmpty(readObjConverterExp) && StringUtils.isNotNull(value))
                        {
                            cell.setCellValue(ServiceUtil.getCommonService().convertByObjExp(String.valueOf(value), readObjConverterExp));
                        }
                        else
                        {
                            cell.setCellType(CellType.STRING);
                            // 如果数据存在就填入,不存在填入空格.
                            cell.setCellValue(StringUtils.isNull(value) ? attr.defaultValue() : value + attr.suffix());
                        }
                    }
                }
                catch (Exception e)
                {
                    log.error("导出Excel失败{}", e);
                }
            }
        }
    }

    /**
     * 设置 POI XSSFSheet 单元格提示
     *
     * @param sheet         表单
     * @param promptTitle   提示标题
     * @param promptContent 提示内容
     * @param firstRow      开始行
     * @param endRow        结束行
     * @param firstCol      开始列
     * @param endCol        结束列
     */
    public void setXSSFPrompt(Sheet sheet, String promptTitle, String promptContent, int firstRow, int endRow,
                              int firstCol, int endCol)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createCustomConstraint("DD1");
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        dataValidation.createPromptBox(promptTitle, promptContent);
        dataValidation.setShowPromptBox(true);
        sheet.addValidationData(dataValidation);
    }

    /**
     * 设置某些列的值只能输入预制的数据,显示下拉框.
     *
     * @param sheet    要设置的sheet.
     * @param textlist 下拉框显示的内容
     * @param firstRow 开始行
     * @param endRow   结束行
     * @param firstCol 开始列
     * @param endCol   结束列.
     * @return 设置好的sheet.
     */
    public void setXSSFValidation(Sheet sheet, String[] textlist, int firstRow, int endRow, int firstCol, int endCol)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // 加载下拉列表内容
        DataValidationConstraint constraint = helper.createExplicitListConstraint(textlist);
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        // 数据有效性对象
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation)
        {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        }
        else
        {
            dataValidation.setSuppressDropDownArrow(false);
        }
        sheet.addValidationData(dataValidation);
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @return 解析后值
     * @throws Exception
     */
    public static String convertByExp(String propertyValue, String converterExp) throws Exception
    {
        try
        {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource)
            {
                String[] itemArray = item.split("=");
                if (itemArray[0].equals(propertyValue))
                {
                    return itemArray[1];
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        return propertyValue;
    }

    /**
     * 反向解析值 男=0,女=1,未知=2
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @return 解析后值
     * @throws Exception
     */
    public static String reverseByExp(String propertyValue, String converterExp) throws Exception
    {
        try
        {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource)
            {
                String[] itemArray = item.split("=");
                if (itemArray[1].equals(propertyValue))
                {
                    return itemArray[0];
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        return propertyValue;
    }

    /**
     * 编码文件名
     */
    public String encodingFilename(String filename)
    {
        filename = UUID.randomUUID().toString() + "_" + filename + ".xlsx";
        return filename;
    }

    /**
     * 获取下载路径
     *
     * @param filename 文件名称
     */
    public String getAbsoluteFile(String filename)
    {
        String downloadPath = ToolUtil.getDownloadPath() + filename;
        File desc = new File(downloadPath);
        if (!desc.getParentFile().exists())
        {
            desc.getParentFile().mkdirs();
        }
        return downloadPath;
    }

    /**
     * 获取bean中的属性值
     *
     * @param vo    实体对象
     * @param field 字段
     * @param excel excel注解
     * @return 最终的属性值
     * @throws Exception
     */
    private Object getTargetValue(T vo, Field field, Excel excel) throws Exception
    {
        Object o = field.get(vo);
        if (StringUtils.isNotEmpty(excel.targetAttr()))
        {
            String target = excel.targetAttr();
            if (target.indexOf(".") > -1)
            {
                String[] targets = target.split("[.]");
                for (String name : targets)
                {
                    o = getValue(o, name);
                }
            }
            else
            {
                o = getValue(o, target);
            }
        }
        return o;
    }

    /**
     * 以类的属性的get方法方法形式获取值
     *
     * @param o
     * @param name
     * @return value
     * @throws Exception
     */
    private Object getValue(Object o, String name) throws Exception
    {
        if (StringUtils.isNotEmpty(name))
        {
            Class<?> clazz = o.getClass();
            String methodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
            Method method = clazz.getMethod(methodName);
            o = method.invoke(o);
        }
        return o;
    }

    /**
     * 得到所有定义字段
     */
    private void createExcelField()
    {
        this.fields = new ArrayList<Field>();
        Class<?> tempClass = clazz;
        List<Field> tempFields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        while (tempClass != null)
        {
            tempClass = tempClass.getSuperclass();
            if (tempClass != null)
            {
                tempFields.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            }
        }
        putToFields(tempFields);
    }

    /**
     * 放到字段集合中
     */
    private void putToFields(List<Field> fields)
    {
        for (Field field : fields)
        {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr != null && (attr.type() == Excel.Type.ALL || attr.type() == type))
            {
                this.fields.add(field);
            }
        }
    }

    /**
     * 创建一个工作簿
     */
    private void createWorkbook()
    {
        this.wb = new SXSSFWorkbook(500);
    }

    /**
     * 获取单元格值
     * @param row 获取的行
     * @param column 获取单元格列号
     * @return 单元格值
     */
    public Object getCellValue(Row row, int column)
    {
        if (row == null)
        {
            return row;
        }
        Object val = "";
        try
        {
            Cell cell = row.getCell(column);
            if (cell != null)
            {
                if (cell.getCellTypeEnum() == CellType.NUMERIC)
                {
                    val = cell.getNumericCellValue();
                    if (HSSFDateUtil.isCellDateFormatted(cell))
                    {
                        val = DateUtil.getJavaDate((Double) val);
                    }
                    else
                    {
                        if ((Double) val % 1 > 0)
                        {
                            val = new DecimalFormat("0.00").format(val);
                        }
                        else
                        {
                            val = new DecimalFormat("0").format(val);
                        }
                    }
                }
                else if (cell.getCellTypeEnum() == CellType.STRING)
                {
                    val = cell.getStringCellValue();
                }
                else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                {
                    val = cell.getBooleanCellValue();
                }
                else if (cell.getCellTypeEnum() == CellType.ERROR)
                {
                    val = cell.getErrorCellValue();
                }
            }
        }
        catch (Exception e)
        {
            return val;
        }
        return val;
    }
}
