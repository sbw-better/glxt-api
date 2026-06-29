package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;

public interface ApiService extends IService<ApiInterface> {

    /**
     * 接口查询
     * @param tenant
     * @param apiCode
     * @return
     */
    ApiInterfaceVO getByApi(String tenant, String apiCode);

    ApiInterface getByCodeName(String code, String name);

    Boolean isHaveConnection(Long connectionId);
}
