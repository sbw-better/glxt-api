package com.citics.glxtapi.common.model.dto;

import com.citics.glxtapi.common.result.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "通用下载请求入参")
public class DownloadDto extends BaseEntity {

    private static final long serialVersionUID = 4746232205056588240L;

    /**
     * 文件名称
     */
    @NotBlank(message = "文件名称必填")
    @ApiModelProperty(value = "文件名称")
    private String fileName;

    /**
     * 是否删除源文件
     */
    @ApiModelProperty(value = "是否删除源文件")
    private Boolean delete;

    /**
     * 是否增加时间戳
     */
    @ApiModelProperty(value = "是否增加时间戳")
    private Boolean timeStamp;

}
