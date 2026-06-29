package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.web.entity.TenantInterface;

import java.io.Serializable;
import java.util.List;

public interface TenantInterfaceService extends IService<TenantInterface> {

    /**
     * 删除
     * @param authId
     * @return
     */
    boolean delete(Serializable authId);

    /**
     * 通过接口ID获取分组ID列表
     * @param apiId
     * @return
     */
    List<TenantInterface> getAuthIdByApiId(Long apiId);

    /**
     * 通过接口ID删除所有包含该接口的分组接口关系
     * @param apiId
     * @return
     */
    boolean deleteAuthIdByApiId(Serializable apiId);
}
