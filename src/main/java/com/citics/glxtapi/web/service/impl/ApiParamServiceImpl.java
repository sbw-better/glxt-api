package com.citics.glxtapi.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.mapper.ApiParamMapper;
import com.citics.glxtapi.web.service.ApiParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;
import static com.citics.glxtapi.web.constants.Constants.*;

@Service
@Slf4j
public class ApiParamServiceImpl extends ServiceImpl<ApiParamMapper, ApiParam> implements ApiParamService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long apiId, List<ApiParam> apiParamList) {
        isFalse(apiId == null, "接口配置ID不能为空，请核对！");
        List<ApiParam> oldApiParamList = this.list(apiId);
        for (ApiParam oldApiParam : oldApiParamList) {
            if (apiParamList.stream().noneMatch(x -> x.getId() != null && x.getId().equals(oldApiParam.getId()))) {
                this.removeById(oldApiParam.getId());
            }
        }
        for (ApiParam apiParam : apiParamList) {
            apiParam.setApiId(apiId);
            if (apiParam.isNew()) {
                isFalse(this.isHaveParam(apiId, apiParam.getCode()), "入参中已经存在【" + apiParam.getCode() + "】参数，请核对！");
                this.save(apiParam);
            } else {
                this.updateById(apiParam);
            }
        }
    }

    @Override
    public List<ApiParam> list(Long apiId) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId);
        wrapper.orderByAsc("id");
        return this.list(wrapper);
    }

    @Override
    public boolean delete(Serializable apiId) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId);
        return this.remove(wrapper);
    }

    @Override
    public boolean isHaveParam(Long apiId, String code) {
        QueryWrapper<ApiParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiParam::getApiId, apiId).eq(ApiParam::getCode, code);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean apiHaveThisParam(Long apiId, String paramCode) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId).eq(ApiParam::getCode, paramCode);
        long count = this.count(wrapper);
        return count > 0
                || paramCode.equals(API_PARAM_MANAGER_ID)
                || paramCode.equals(API_PARAM_PRODUCT_ID_LIST)
                || paramCode.equals(API_PARAM_PRODUCT_CODE_LIST);
    }

    @Override
    public boolean requiredParamIsFull(Long apiId, List<String> paramCodeList) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId).eq(ApiParam::getRequired, WHETHER_YES);
        if (!CollectionUtils.isEmpty(paramCodeList)) {
            wrapper.lambda().notIn(ApiParam::getCode, paramCodeList);
        }
        long count = this.count(wrapper);
        return count < 1;
    }

    @Override
    public List<ApiParam> notRequiredParamList(Long apiId, List<String> paramCodeList) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId).eq(ApiParam::getRequired, WHETHER_NO)
                .isNotNull(ApiParam::getDefaultValue);
        paramCodeList.add(API_PARAM_MANAGER_ID);
        paramCodeList.add(API_PARAM_PRODUCT_ID_LIST);
        paramCodeList.add(API_PARAM_PRODUCT_CODE_LIST);
        wrapper.lambda().notIn(ApiParam::getCode, paramCodeList);
        return this.list(wrapper);
    }

    @Override
    public List<ApiParam> listByCodeList(Long apiId, List<String> paramCodeList) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId);
        if (!CollectionUtils.isEmpty(paramCodeList) && paramCodeList.size() > 0) {
            wrapper.lambda().in(ApiParam::getCode, paramCodeList);
        } else {
            wrapper.lambda().eq(ApiParam::getApiId, -999);
        }
        return this.list(wrapper);
    }

    @Override
    public boolean apiHaveRequiredParam(Long apiId) {
        QueryWrapper<ApiParam> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiParam::getApiId, apiId).eq(ApiParam::getRequired, WHETHER_YES);
        List<ApiParam> paramList = this.list(wrapper);
        return (null != paramList && paramList.size() != 0);
    }
}
