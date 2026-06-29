package com.citics.glxtapi.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.web.entity.TenantInterface;
import com.citics.glxtapi.web.mapper.TenantInterfaceMapper;
import com.citics.glxtapi.web.service.TenantInterfaceService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class TenantInterfaceServiceImpl extends ServiceImpl<TenantInterfaceMapper, TenantInterface> implements TenantInterfaceService {

    @Override
    public boolean delete(Serializable authId) {
        QueryWrapper<TenantInterface> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TenantInterface::getAuthId, authId);
        return this.remove(wrapper);
    }

    @Override
    public List<TenantInterface> getAuthIdByApiId(Long apiId) {
        QueryWrapper<TenantInterface> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TenantInterface::getApiId, apiId);
        return this.list(wrapper);
    }

    @Override
    public boolean deleteAuthIdByApiId(Serializable apiId) {
        QueryWrapper<TenantInterface> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TenantInterface::getApiId, apiId);
        return this.remove(wrapper);
    }
}
