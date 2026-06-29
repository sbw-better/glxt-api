package com.citics.glxtapi.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.common.factory.PageFactory;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.utils.page.PageUtils;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.TenantAuth;
import com.citics.glxtapi.web.entity.TenantInterface;
import com.citics.glxtapi.web.entity.dto.TenantAuthDTO;
import com.citics.glxtapi.web.entity.dto.TenantInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.TenantInterfaceVO;
import com.citics.glxtapi.web.mapper.TenantAuthMapper;
import com.citics.glxtapi.web.service.ApiService;
import com.citics.glxtapi.web.service.TenantAuthService;
import com.citics.glxtapi.web.service.TenantInterfaceService;
import com.citics.glxtapi.web.support.TenantContextHolder;
import com.citics.glxtapi.web.support.UserContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;

@Service
public class TenantAuthServiceImpl extends ServiceImpl<TenantAuthMapper, TenantAuth> implements TenantAuthService {

    @Autowired
    private TenantInterfaceService tenantInterfaceService;
    @Autowired
    private ApiService apiService;

    @Override
    public TenantAuth add(TenantAuth tenantAuth) {
        isFalse(StringUtils.isEmpty(tenantAuth.getName()), "分组名称不能为空，请核对！");
        // 设置租户信息
        String tenant = TenantContextHolder.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            tenantAuth.setTenant(tenant);
        }

        if (tenantAuth.isNew()) {
            // 生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            tenantAuth.setToken(token);
            tenantAuth.setCreateTime(new Date());
            tenantAuth.setCreateBy(UserContextHolder.getUserId());
            this.save(tenantAuth);
        } else {
            TenantAuth oldTenantAuth = this.getById(tenantAuth.getId());
            isFalse(null == oldTenantAuth, "未找到对应租户权限分组，无法更新，请核对！");
            tenantAuth.setToken(oldTenantAuth.getToken());
            tenantAuth.setUpdateTime(new Date());
            tenantAuth.setUpdateBy(UserContextHolder.getUserId());
            this.updateById(tenantAuth);
        }
        return tenantAuth;
    }

    @Override
    public TenantAuth saveToken(TenantAuth tenantAuth) {
        isFalse(null == tenantAuth.getId(), "分组ID不能为空，请核对！");
        TenantAuth oldTenantAuth = this.getById(tenantAuth.getId());
        isFalse(null == oldTenantAuth, "未找到对应租户权限分组，无法更新token，请核对！");
        QueryWrapper<TenantAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TenantAuth::getToken, tenantAuth.getToken()).ne(TenantAuth::getId, tenantAuth.getId());
        isFalse(this.count(queryWrapper) > 0, "其他分组已存在token，无法更新，请核对！");
        tenantAuth.setUpdateTime(new Date());
        tenantAuth.setUpdateBy(UserContextHolder.getUserId());
        this.updateById(tenantAuth);
        return tenantAuth;
    }

    @Override
    public List<TenantAuth> authList() {
        QueryWrapper<TenantAuth> queryWrapper = new QueryWrapper<>();
        // 设置租户信息
        String tenant = TenantContextHolder.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(TenantAuth::getTenant, tenant);
        }
        queryWrapper.orderByAsc("id");
        return this.list(queryWrapper);
    }

    @Override
    public PageResult authPage(TenantAuthDTO dto) {
        QueryWrapper<TenantAuth> queryWrapper = new QueryWrapper<>();
        // 设置租户信息
        String tenant = TenantContextHolder.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(TenantAuth::getTenant, tenant);
        }

        // 分组名称模糊匹配
        queryWrapper.lambda().like(StringUtils.isNotEmpty(dto.getName()), TenantAuth::getName, dto.getName());
        // 分组描述模糊匹配
        queryWrapper.lambda().like(StringUtils.isNotEmpty(dto.getDescription()), TenantAuth::getDescription, dto.getDescription());

        // 接口名称、接口代码匹配
        if (StringUtils.isNotEmpty(dto.getApiCode()) || StringUtils.isNotEmpty(dto.getApiName())) {
            ApiInterface apiInterface = this.apiService.getByCodeName(dto.getApiCode(), dto.getApiName());
            if (null != apiInterface) {
                List<TenantInterface> authList = this.tenantInterfaceService.getAuthIdByApiId(apiInterface.getId());
                if (null != authList && authList.size() != 0) {
                    List<Long> collect = authList.stream().map(TenantInterface::getAuthId).collect(Collectors.toList());
                    queryWrapper.lambda().in(TenantAuth::getId, collect);
                } else {
                    queryWrapper.lambda().eq(TenantAuth::getId, -99);
                }
            } else {
                queryWrapper.lambda().eq(TenantAuth::getId, -99);
            }
        }

        queryWrapper.orderByAsc("id");
        return PageUtils.getPageResult(this.page(PageFactory.jsonPage(dto.getPageNum(), dto.getPageSize()), queryWrapper));
    }

    @Override
    public boolean delete(Serializable id) {
        TenantAuth tenantAuth = this.getById(id);
        isFalse(null == tenantAuth, "未找到对应租户权限分组，无法删除，请核对！");
        boolean res = this.removeById(id);
        boolean subRes = this.tenantInterfaceService.delete(id);
        return res && subRes;
    }

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    @Override
    public Boolean isHaveTenant(String tenant) {
        QueryWrapper<TenantAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TenantAuth::getTenant, tenant);
        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInterface(TenantInterfaceDTO dto) {
        Long authId = dto.getAuthId();
        List<TenantInterface> tenantInterfaceList = dto.getTenantInterfaceList();
        List<TenantAuth> tenantAuthList = this.authList();
        isFalse(authId == null, "分组ID不能为空，请核对！");
        isFalse(tenantAuthList.stream().noneMatch(x->x.getId().equals(authId)), "无该分组权限，请核对！");

        List<TenantInterfaceVO> oldTenantInterfaceList = this.listInterface(authId);
        for (TenantInterfaceVO oldTenantInterface : oldTenantInterfaceList) {
            if (tenantInterfaceList.stream().noneMatch(x->x.getId() != null && x.getId().equals(oldTenantInterface.getId()))) {
                this.tenantInterfaceService.removeById(oldTenantInterface.getId());
            }
        }

        for (TenantInterface tenantInterface : tenantInterfaceList) {
            tenantInterface.setAuthId(authId);
            if (tenantInterface.isNew()) {
                this.tenantInterfaceService.save(tenantInterface);
            } else {
                this.tenantInterfaceService.updateById(tenantInterface);
            }
        }
    }

    @Override
    public List<TenantInterfaceVO> listInterface(Long authId) {
        List<TenantAuth> tenantAuthList = this.authList();
        isFalse(tenantAuthList.stream().noneMatch(x->x.getId().equals(authId)), "无该分组权限，请核对！");

        QueryWrapper<TenantInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TenantInterface::getAuthId, authId);
        queryWrapper.orderByAsc("id");
        List<TenantInterface> tenantInterfaceList = this.tenantInterfaceService.list(queryWrapper);

        List<TenantInterfaceVO> voList = new ArrayList<>();
        for (TenantInterface tenantInterface : tenantInterfaceList) {
            TenantInterfaceVO vo = new TenantInterfaceVO();
            vo.setId(tenantInterface.getId());
            vo.setAuthId(tenantInterface.getAuthId());
            vo.setApiId(tenantInterface.getApiId());
            ApiInterface apiInterface = apiService.getById(tenantInterface.getApiId());
            if (apiInterface != null) {
                vo.setApiName(apiInterface.getName());
                vo.setApiCode(apiInterface.getCode());
                vo.setPreviewSql(apiInterface.getPreviewSql());
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public boolean hasExecutePermission(Long apiId, String token, String ip) {
        if (StringUtils.isEmpty(token) || null == apiId) {
            return false;
        }

        // 根据token获取接口权限分组配置
        QueryWrapper<TenantAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TenantAuth::getToken, token);
        TenantAuth tenantAuth = this.getOne(queryWrapper);
        if (null == tenantAuth) {
            return false;
        }

        // 检查IP配置
        String ipConfigStr = tenantAuth.getIpConfig();
        if (StringUtils.isNotEmpty(ipConfigStr)) {
            if (StringUtils.isEmpty(ip)) {
                return false;
            }
            JSONArray jsonArray = JSON.parseArray(ipConfigStr);
            boolean ipMatch = false;
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("ip").trim().equals(ip.trim())) {
                    ipMatch = true;
                    break;
                }
            }
            if (!ipMatch) {
                return false;
            }
        }

        // 检查是否具备该接口权限
        QueryWrapper<TenantInterface> apiQueryWrapper = new QueryWrapper<>();
        apiQueryWrapper.lambda().eq(TenantInterface::getAuthId, tenantAuth.getId());
        List<TenantInterface> tenantInterfaceList = this.tenantInterfaceService.list(apiQueryWrapper);

        return !tenantInterfaceList.isEmpty() && tenantInterfaceList.stream().anyMatch(x -> x.getApiId().equals(apiId));
    }
}
