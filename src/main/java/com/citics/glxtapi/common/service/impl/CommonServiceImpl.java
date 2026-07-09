package com.citics.glxtapi.common.service.impl;

import com.citics.glxtapi.common.entity.Dictionary;
import com.citics.glxtapi.common.mapper.DictionaryMapper;
import com.citics.glxtapi.common.service.CommonService;
import com.citics.glxtapi.common.utils.string.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class CommonServiceImpl implements CommonService {

    @Resource
    private DictionaryMapper dictionaryMapper;

    @Override
    public List<com.citics.glxtapi.common.entity.Dictionary> getDicList(Map<String, Object> param)
    {
        List<Dictionary> dictionaryList = dictionaryMapper.selectDictionary(param);
        return dictionaryList;
    }

    /**
     * 根据FLDM解析字典值
     */
    @Override
    public String convertByDicExp(String propertyValue, String converterExp)
    {
        Map<String, Object> param = new HashMap<>();
        param.put("fldm", converterExp);
        String[] ibms = propertyValue.trim().split(";");
        if (StringUtils.isEmpty(ibms)) {
            return "";
        }
        param.put("ibm", ibms);
        List<Dictionary> dictionaryList = getDicList(param);
        Map<Integer, String> map = new HashMap<>();
        for (com.citics.glxtapi.common.entity.Dictionary dictionary : dictionaryList) {
            map.put(dictionary.getIbm(), dictionary.getNote());
        }
        StringBuilder result = new StringBuilder();
        for (String ibm : ibms) {
            if (result.length() > 0) {
                result.append(";");
            }
            if (StringUtils.isEmpty(ibm) || StringUtils.isEmpty(map.get(Integer.valueOf(ibm)))) {
                result.append(ibm);
            } else {
                result.append(map.get(Integer.valueOf(ibm)));
            }
        }
        return StringUtils.isEmpty(result.toString()) ? "" : result.toString();
    }

    /**
     * 根据内部对象解析展示值
     */
    @Override
    public String convertByObjExp(String propertyValue, String converterExp)
    {
        String[] convertSource = converterExp.split(",");
        String tableName = "";
        String display = "";
        String displayMore = null;
        String key = "ID";
        String split = ";";
        for (String item : convertSource)
        {
            String[] itemArray = item.split("-");
            switch (itemArray[0]) {
                case "table":
                    tableName = itemArray[1];
                    break;
                case "display":
                    display = itemArray[1];
                    break;
                case "displayMore":
                    displayMore = itemArray[1];
                    break;
                case "key":
                    key = itemArray[1];
                    break;
                case "split":
                    split = itemArray[1];
                    break;
            }
        }

        String[] keyVals = propertyValue.trim().split(";");
        if (StringUtils.isEmpty(keyVals)) {
            return "";
        }

        List<com.citics.glxtapi.common.entity.Dictionary> dictionaryList = dictionaryMapper.selectDisplayValueByKey(display, tableName, key, keyVals);
        List<com.citics.glxtapi.common.entity.Dictionary> dictionaryMoreList = null;
        if (!StringUtils.isEmpty(displayMore)) {
            dictionaryMoreList = dictionaryMapper.selectDisplayValueByKey(displayMore, tableName, key, keyVals);
        }

        Map<String, String> map = new HashMap<>();
        for (com.citics.glxtapi.common.entity.Dictionary dictionary : dictionaryList) {
            map.put(dictionary.getCbm(), dictionary.getNote());
            if (!StringUtils.isEmpty(displayMore)) {
                for (com.citics.glxtapi.common.entity.Dictionary dicMore : dictionaryMoreList) {
                    if (dicMore.getCbm().equals(dictionary.getCbm())) {
                        map.put(dictionary.getCbm(), dictionary.getNote() + " " + dicMore.getNote());
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (String keyVal : keyVals) {
            if (result.length() > 0) {
                result.append(split);
            }
            if (StringUtils.isEmpty(keyVal) || StringUtils.isEmpty(map.get(keyVal))) {
                result.append(keyVal);
            } else {
                result.append(map.get(keyVal));
            }
        }
        return StringUtils.isEmpty(result.toString()) ? "" : result.toString();
    }
}
