package com.citics.glxtapi.common.controller;

import com.citics.glxtapi.common.annotation.MethodLog;
import com.citics.glxtapi.common.model.dto.DownloadDto;
import com.citics.glxtapi.common.result.ResultModel;
import com.citics.glxtapi.common.utils.date.DateUtils;
import com.citics.glxtapi.common.utils.file.FileUtils;
import com.citics.glxtapi.common.utils.file.ToolUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通用 - 通用接口
 *
 * @author wz
 * @date 2022-07-05
 */
@Slf4j
@Api(tags = {"通用接口"})
@Controller
@RequestMapping("/api/common")
public class CommonController {


    /**
     * 通用下载请求
     *
     * @param dto
     * @param req
     * @param resp
     * @return
     * @apiNote 从本地临时目录下载文件
     */
    @ApiOperation(value = "通用下载接口")
    @MethodLog(desc = "通用下载接口")
    @PostMapping("/download")
    public ResultModel<?> download(@Validated @RequestBody DownloadDto dto, HttpServletRequest req, HttpServletResponse resp) {
        String fileName = dto.getFileName();
        Boolean delete = dto.getDelete();
        Boolean timeStamp = dto.getTimeStamp();
        try {
            String filePath = ToolUtil.getDownloadPath() + fileName;
            String realFileName = fileName.substring(fileName.indexOf("_") + 1);
            if (timeStamp == null || timeStamp) {
                realFileName = DateUtils.dateTimeNow() + realFileName;
            }

            resp.setCharacterEncoding("utf-8");
            resp.setContentType("application/octet-stream");
            String fileDownloadHeader = FileUtils.setFileDownloadHeader(req, realFileName);
            resp.setHeader("Content-Disposition",
                    "attachment;filename=" + fileDownloadHeader);
            resp.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            FileUtils.writeBytes(filePath, resp.getOutputStream());
            if (delete == null || delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (IOException e) {
            log.error("文件下载失败，filename={}, err={}", fileName, e.getMessage());
            e.printStackTrace();
            return ResultModel.error("下载失败！");
        }
        return ResultModel.success();
    }

}
