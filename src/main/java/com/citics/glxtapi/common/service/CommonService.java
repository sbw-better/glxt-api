package com.citics.glxtapi.common.service;


import com.citics.glxtapi.common.entity.Dictionary;

import java.util.List;
import java.util.Map;

public interface CommonService {

    List<Dictionary> getDicList(Map<String, Object> param);

    /**
     * 字典解析
     */
    String convertByDicExp(String propertyValue, String converterExp);

    /**
     * 内部对象解析
     */
    String convertByObjExp(String propertyValue, String converterExp);

}
