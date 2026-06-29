package com.citics.glxtapi.web.entity.dto;

import com.citics.glxtapi.web.entity.ApiActuator;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value="执行器dto", description="")
public class ApiActuatorDTO extends ApiActuator {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码（起始页为1）")
    private int pageNum = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty(value = "每页数量")
    private int pageSize = 0;

    @ApiModelProperty("执行开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date executeTimeStart;

    @ApiModelProperty("执行结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date executeTimeEnd;
}
