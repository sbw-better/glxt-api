package com.citics.glxtapi.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citics.glxtapi.web.entity.TenantAuth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantAuthMapper extends BaseMapper<TenantAuth> {
}
