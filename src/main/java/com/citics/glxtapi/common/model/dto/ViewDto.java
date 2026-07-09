package com.citics.glxtapi.common.model.dto;

import com.citics.glxtapi.common.result.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "通用预览请求入参")
public class ViewDto extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 文件夹名
     */
    @ApiModelProperty(value = "文件夹名")
    private String folder;

    /**
     * 后缀
     */
    @ApiModelProperty(value = "后缀")
    private String suffix;

    /**
     * ID
     */
    @ApiModelProperty(value = "ID")
    private Long id;

}
