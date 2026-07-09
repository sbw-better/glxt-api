package com.citics.glxtapi.common.mapper;

import com.citics.glxtapi.common.entity.Dictionary;

import java.util.List;
import java.util.Map;

public interface DictionaryMapper {

    List<Dictionary> selectDictionary(Map<String, Object> params);

    List<Dictionary> selectDisplayValueByID(String display, String tableName, String[] id);

    List<Dictionary> selectIDByDisplayValue(String display, String tableName, String[] displayVals);

    List<Dictionary> selectDisplayValueByKey(String display, String tableName, String key, String[] keyVals);

    List<Dictionary> selectKeyByDisplayValue(String key, String display, String tableName, String[] displayVals);

}
