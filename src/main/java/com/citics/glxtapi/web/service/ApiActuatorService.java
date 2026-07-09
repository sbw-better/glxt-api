package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.web.entity.ApiActuator;
import com.citics.glxtapi.web.entity.dto.ApiActuatorDTO;

import javax.servlet.http.HttpServletRequest;

public interface ApiActuatorService extends IService<ApiActuator> {

    Object execute(String apiActuatorInfo, HttpServletRequest req);

    String executeExport(String apiActuatorInfo, HttpServletRequest req);

    PageResult page(ApiActuatorDTO dto);

    void insertAfterExecute(String apiActuatorInfo, HttpServletRequest req, boolean success, Object sqlRes, long consumeTime);

    boolean executeCheck(ApiActuatorDTO dto);
}
