package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.web.entity.ApiParam;

import java.io.Serializable;
import java.util.List;

public interface ApiParamService extends IService<ApiParam> {

    /**
     * 检测是否已存在
     * @param apiId
     * @param code
     * @return
     */
    boolean isHaveParam(Long apiId, String code);

    /**
     * 保存或修改或删除入参
     * @param apiId
     * @param apiParamList
     */
    void save(Long apiId, List<ApiParam> apiParamList);

    /**
     * 列表查询
     * @param apiId
     * @return
     */
    List<ApiParam> list(Long apiId);

    /**
     * 删除
     * @param apiId
     * @return
     */
    boolean delete(Serializable apiId);

    /**
     * 判断接口配置中是否有代码为paramCode的参数
     * @param apiId
     * @param paramCode
     */
    boolean apiHaveThisParam(Long apiId, String paramCode);

    /**
     * 判断接口配置中所有的必填参数，是否在paramCodeList中齐全了
     * @param apiId
     * @param paramCodeList
     */
    boolean requiredParamIsFull(Long apiId, List<String> paramCodeList);

    /**
     * 接口配置中所有的非必填且有默认值且不在paramCodeList中的参数
     * @param apiId
     * @param paramCodeList
     */
    List<ApiParam> notRequiredParamList(Long apiId, List<String> paramCodeList);

    /**
     * 通过参数代码查询参数列表
     * @param apiId
     * @param paramCodeList
     */
    List<ApiParam> listByCodeList(Long apiId, List<String> paramCodeList);

    /**
     * 判断接口中是否有配置必填的参数
     * @param apiId
     */
    boolean apiHaveRequiredParam(Long apiId);
}
