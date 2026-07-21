package com.citics.glxtapi.web.controller;

import com.alibaba.fastjson.JSON;
import com.citics.glxtapi.common.annotation.MethodLog;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.result.ResultModel;
import com.citics.glxtapi.plugin.db.exception.OpenException;
import com.citics.glxtapi.web.entity.dto.ApiActuatorDTO;
import com.citics.glxtapi.web.entity.vo.ApiActuatorExcelResult;
import com.citics.glxtapi.web.service.ApiActuatorService;
import com.citics.glxtapi.web.support.ApiPreHandle;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;

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
    public Object execute(@RequestBody String apiActuatorInfo, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long consumeTime;
        long startTime = System.currentTimeMillis();
        Integer res = SQL_EXECUTE_SUCCESS;
        Object executeResultDetail = null;
        String msg = "";
        boolean exportExcel = false;
        ApiActuatorExcelResult excelResult = null;
        try {
            // exportExcel默认按false处理，避免影响未传该字段的老调用方。
            exportExcel = Boolean.TRUE.equals(JSON.parseObject(apiActuatorInfo).getBoolean("exportExcel"));
            if (exportExcel) {
                excelResult = apiActuatorService.executeExcel(apiActuatorInfo, req);
            } else {
                executeResultDetail = apiActuatorService.execute(apiActuatorInfo, req);
            }
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
            if (exportExcel) {
                writeExcelResponse(excelResult, resp);
                return null;
            }
            return ResultModel.success(executeResultDetail);
        } else if (res.equals(SQL_EXECUTE_FAIL)) {
            return ResultModel.error("操作失败：" + msg, executeResultDetail);
        } else {
            return ResultModel.error("执行时出错，或SQL语句可能存在注入风险，请联系管理员处理！" + msg);
        }
    }

    private void writeExcelResponse(ApiActuatorExcelResult excelResult, HttpServletResponse resp) throws Exception {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 暴露Content-Disposition，前端可从响应头中读取下载文件名。
        String fileName = URLEncoder.encode(excelResult.getFileName(), "UTF-8").replace("+", "%20");
        resp.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        resp.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        resp.setContentLength(excelResult.getContent().length);
        resp.getOutputStream().write(excelResult.getContent());
        resp.flushBuffer();
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
