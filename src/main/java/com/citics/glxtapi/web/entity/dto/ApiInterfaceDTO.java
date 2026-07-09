package com.citics.glxtapi.web.entity.dto;

import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.ApiParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ApiInterfaceDTO extends ApiInterface implements Serializable {

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

    /**
     * 接口参数
     */
    @ApiModelProperty(value = "接口参数子表")
    private List<ApiParam> apiParamList;

    /**
     * ID串
     */
    @ApiModelProperty(value = "ID串")
    private List<Long> ids;
}
