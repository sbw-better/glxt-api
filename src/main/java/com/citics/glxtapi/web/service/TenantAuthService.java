package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.web.entity.TenantAuth;
import com.citics.glxtapi.web.entity.dto.TenantAuthDTO;
import com.citics.glxtapi.web.entity.dto.TenantInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.TenantInterfaceVO;

import java.io.Serializable;
import java.util.List;

public interface TenantAuthService extends IService<TenantAuth> {

    /**
     * 保存租户人员关系
     * @param tenantUser
     * @return
     */
    TenantAuth add(TenantAuth tenantUser);

    /**
     * 保存分组token
     * @param tenantUser
     * @return
     */
    TenantAuth saveToken(TenantAuth tenantUser);

    /**
     * 获取授权租户列表
     */
    List<TenantAuth> authList();

    /**
     * 获取授权租户列表分页
     */
    PageResult authPage(TenantAuthDTO dto);

    /**
     * 删除租户用户关系
     * @param id
     * @return
     */
    boolean delete(Serializable id);

    /**
     * 保存/更改/删除租户权限接口关系
     * @param dto
     * @return
     */
    void saveInterface(TenantInterfaceDTO dto);

    /**
     * 获取授权租户权限接口关系
     */
    List<TenantInterfaceVO> listInterface(Long authId);

    /**
     * 检测接口调用权限
     * @param apiId
     * @param token
     * @param ip
     * @return
     */
    boolean hasExecutePermission(Long apiId, String token, String ip);

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    Boolean isHaveTenant(String tenant);
}
