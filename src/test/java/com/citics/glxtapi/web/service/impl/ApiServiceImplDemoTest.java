package com.citics.glxtapi.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.InterfaceDemoVo;
import com.citics.glxtapi.web.support.TenantContextHolder;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ApiServiceImplDemoTest {

    @BeforeClass
    public static void initializeLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApiInterface.class);
    }

    @After
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    public void demoUsesCurrentTenantAndDoesNotExposeProcedureName() {
        ApiServiceImpl service = spy(new ApiServiceImpl());
        ReflectionTestUtils.setField(service, "demoPath", "/api/actuator/execute");

        ApiInterface api = new ApiInterface();
        api.setId(1L);
        api.setCode("sameCode");
        api.setType(2);
        api.setProcedureName("PKG_PRIVATE.QUERY_DATA");
        doReturn(api).when(service).getOne(any(QueryWrapper.class));

        ApiInterfaceVO apiInterfaceVO = new ApiInterfaceVO();
        doReturn(apiInterfaceVO).when(service).get(1L, "sameCode");
        TenantContextHolder.setTenant("tenantA");

        ApiInterfaceDTO request = new ApiInterfaceDTO();
        request.setCode("sameCode");
        InterfaceDemoVo demo = service.demo(request);

        assertFalse(JSON.parseObject(demo.getBody()).containsKey("procedureName"));
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(service).getOne(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().toLowerCase().contains("tenant"));
    }
}
