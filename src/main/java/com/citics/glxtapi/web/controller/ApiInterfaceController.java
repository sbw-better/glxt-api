package com.citics.glxtapi.web.controller;

import com.citics.glxtapi.common.annotation.MethodLog;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.result.ResultModel;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.InterfaceDemoVo;
import com.citics.glxtapi.web.service.ApiService;
import com.citics.glxtapi.web.support.ApiPreHandle;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@Api(value = "接口配置", tags = "接口配置")
@Validated
@RestController
@RequestMapping("/api/interface")
public class ApiInterfaceController {

    @Resource
    ApiService apiService;

    @ApiPreHandle(minAccessRole = "developer")
    @ApiOperation(value = "保存或更新接口配置及其参数", notes = "保存或更新接口配置及其参数")
    @MethodLog(desc = "保存或更新接口配置及其参数")
    @ResponseBody
    @PostMapping("/save")
    public ResultModel<ApiInterfaceVO> save(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.save(dto));
    }

    @ApiPreHandle(minAccessRole = "maintainer")
    @ApiOperation(value = "删除接口配置", notes = "删除接口配置")
    @MethodLog(desc = "删除接口配置")
    @ResponseBody
    @PostMapping("/delete")
    public ResultModel<Boolean> delete(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.delete(dto.getId()));
    }

    @ApiPreHandle(minAccessRole = "guest")
    @ApiOperation(value = "查询一条接口配置及其参数", notes = "查询一条接口配置及其参数")
    @MethodLog(desc = "查询一条接口配置及其参数")
    @ResponseBody
    @PostMapping("/get")
    public ResultModel<ApiInterfaceVO> getById(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.get(dto.getId(), dto.getCode()));
    }

    @ApiPreHandle(minAccessRole = "guest")
    @ApiOperation(value = "列表查询", notes = "列表查询")
    @MethodLog(desc = "列表查询")
    @ResponseBody
    @PostMapping("/list")
    public ResultModel<List<ApiInterface>> list(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.list(dto));
    }

    @ApiPreHandle(minAccessRole = "guest")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @MethodLog(desc = "分页查询")
    @ResponseBody
    @PostMapping("/page")
    public ResultModel<PageResult> page(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.page(dto));
    }

    @ApiPreHandle(minAccessRole = "guest")
    @ApiOperation(value = "接口SQL预览", notes = "接口SQL预览")
    @MethodLog(desc = "接口SQL预览")
    @ResponseBody
    @PostMapping("/preview")
    public ResultModel<String> preview(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.preview(dto));
    }

    @ApiPreHandle(minAccessRole = "developer")
    @ApiOperation(value = "获取接口http调用demo", notes = "获取接口http调用demo")
    @MethodLog(desc = "获取接口http调用demo")
    @ResponseBody
    @PostMapping("/demo")
    public ResultModel<InterfaceDemoVo> demo(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.demo(dto));
    }

    @ApiOperation(value = "接口是否分页", notes = "接口是否分页")
    @MethodLog(desc = "接口是否分页")
    @ResponseBody
    @PostMapping("/have_page")
    public ResultModel<Boolean> isPage(@RequestBody ApiInterfaceDTO dto) {
        return ResultModel.success(this.apiService.isPage(dto));
    }

    /**
     * 接口配置-导出EXCEL
     */
    @ApiPreHandle(minAccessRole = "developer")
    @ApiOperation(value = "接口配置-导出EXCEL")
    @MethodLog(desc = "接口配置-导出EXCEL")
    @ResponseBody
    @PostMapping("/export")
    public ResultModel<?> export(@RequestBody ApiInterfaceDTO dto) {
        String res = null;
        try {
            res = this.apiService.export(dto);
            return ResultModel.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultModel.error("导出EXCEL失败，" + e.getMessage());
        }
    }

    /**
     * 接口配置-导入EXCEL
     */
    @ApiPreHandle(minAccessRole = "developer")
    @ApiOperation(value = "接口配置-导入EXCEL")
    @MethodLog(desc = "接口配置-导入EXCEL")
    @PostMapping("/upload")
    public ResultModel<Boolean> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (this.apiService.upload(file)) {
                return ResultModel.success("文件上传成功，接下来需要手动修改接口的数据源信息和分组配置信息。", true);
            } else {
                return ResultModel.error("导入EXCEL失败!", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultModel.error("导入EXCEL失败，" + e.getMessage(), false);
        }
    }
}
