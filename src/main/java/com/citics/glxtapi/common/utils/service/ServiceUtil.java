package com.citics.glxtapi.common.utils.service;

import com.citics.glxtapi.common.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/**
 * 服务util
 * 无法自动注入时，引入该依赖实现注入
 *
 * @author wangzhe
 */
@Slf4j
@Component
public class ServiceUtil {

    @Autowired
    private CommonService commonService;

    @Autowired
    private static ServiceUtil serviceUtil;

    @PostConstruct
    public void init() {
        serviceUtil = this;
    }

    public static CommonService getCommonService() {
        return serviceUtil.commonService;
    }

}
