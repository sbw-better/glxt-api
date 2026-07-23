package com.citics.glxtapi.web.entity.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProcedureExecuteResult {

    private Map<String, Object> outParams = new LinkedHashMap<>();

    private Map<String, List<Map<String, Object>>> cursors = new LinkedHashMap<>();

    private Integer resultCount = 0;
}
