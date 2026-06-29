package com.citics.glxtapi.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.web.entity.Tenant;
import com.citics.glxtapi.web.mapper.TenantMapper;
import com.citics.glxtapi.web.service.TenantService;
import com.citics.glxtapi.web.support.DsContextHolder;
import com.citics.glxtapi.web.support.TenantContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Override
    public void clearDs() {
        DsContextHolder.clear();
    }

    @Override
    public void setDs(String ds) {
        DsContextHolder.setDs(ds);
    }

    @Override
    public String getDs() {
        return DsContextHolder.getDs();
    }

    @Override
    public String getTenant() {
        return TenantContextHolder.getTenant();
    }
}
