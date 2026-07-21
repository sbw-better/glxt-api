package com.citics.glxtapi.web.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * /execute导出Excel时在Service和Controller之间传递的文件结果。
 */
@Data
@AllArgsConstructor
public class ApiActuatorExcelResult {

    private String fileName;

    private byte[] content;
}
