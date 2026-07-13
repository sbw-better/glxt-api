package com.citics.glxtapi.web.controller;

import com.citics.glxtapi.common.annotation.MethodLog;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.result.ResultModel;
import com.citics.glxtapi.plugin.db.exception.OpenException;
import com.citics.glxtapi.web.entity.dto.ApiActuatorDTO;
import com.citics.glxtapi.web.service.ApiActuatorService;
import com.citics.glxtapi.web.support.ApiPreHandle;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.citics.glxtapi.web.constants.Constants.*;

@Api(value = "接口调用", tags = "接口调用")
@Validated
@RestController
@RequestMapping("/api/actuator")
public class ApiActuatorController {

    @Resource
    ApiActuatorService apiActuatorService;

    @ApiOperation(value = "执行SQL", notes = "执行SQL")
    @MethodLog(desc = "执行SQL")
    @ResponseBody
    @PostMapping("/execute")
    public ResultModel<?> execute(@RequestBody String apiActuatorInfo, HttpServletRequest req) {
        long consumeTime;
        long startTime = System.currentTimeMillis();
        Integer res = SQL_EXECUTE_SUCCESS;
        Object executeResultDetail = null;
        String msg = "";
        try {
            executeResultDetail = apiActuatorService.execute(apiActuatorInfo, req);
        } catch (OpenException e) {
            e.printStackTrace();
            msg = e.getMessage();
            res = SQL_EXECUTE_FAIL;
        } catch (Exception e) {
            e.printStackTrace();
            msg = e.getMessage();
            res = SQL_EXECUTE_FAIL_PRIVATE;
        } finally {
            consumeTime = System.currentTimeMillis() - startTime;
        }

        this.apiActuatorService.insertAfterExecute(apiActuatorInfo, req, res.equals(SQL_EXECUTE_SUCCESS), res.equals(SQL_EXECUTE_SUCCESS)?null:msg, consumeTime);
        if (res.equals(SQL_EXECUTE_SUCCESS)) {
            return ResultModel.success(executeResultDetail);
        } else if (res.equals(SQL_EXECUTE_FAIL)) {
            return ResultModel.error("操作失败：" + msg, executeResultDetail);
        } else {
            return ResultModel.error("执行时出错，或SQL语句可能存在注入风险，请联系管理员处理！" + msg);
        }
    }

    @ApiOperation(value = "测试SQL前校验", notes = "测试SQL前校验")
    @MethodLog(desc = "测试SQL前校验")
    @ResponseBody
    @PostMapping("/execute_check")
    public ResultModel<Boolean> executeCheck(@RequestBody ApiActuatorDTO dto) {
        return ResultModel.success(apiActuatorService.executeCheck(dto));
    }


    @ApiPreHandle(minAccessRole = "guest")
    @ApiOperation(value = "分页查询调用记录", notes = "分页查询调用记录")
    @MethodLog(desc = "分页查询调用记录")
    @ResponseBody
    @PostMapping("/page")
    public ResultModel<PageResult> getPage(@RequestBody ApiActuatorDTO dto) {
        return ResultModel.success(apiActuatorService.page(dto));
    }
}
