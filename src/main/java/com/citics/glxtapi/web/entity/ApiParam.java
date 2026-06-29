package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("API_SQL_INTERFACE_PARAM")
@NoArgsConstructor
@KeySequence(value = "SEQ_API_SQL_INTERFACE_PARAM")
@ApiModel(value = "接口参数", description = "")
public class ApiParam extends Model<ApiParam> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
    @ApiModelProperty("主键")
    @Excel(name = "参数ID")
    private Long id;

    @JsonIgnore
    public boolean isNew() {
        if (null == id || -1L == id) {
            return true;
        }
        return false;
    }

    @TableField("API_ID")
    @ApiModelProperty("接口ID")
    @Excel(name = "接口ID")
    private Long apiId;

    @TableField("NAME")
    @ApiModelProperty("名称")
    @Excel(name = "参数名称")
    private String name;

    @TableField("CODE")
    @ApiModelProperty("编码")
    @Excel(name = "参数编码")
    private String code;

    @TableField("TYPE")
    @ApiModelProperty("类型")
    @Excel(name = "参数类型")
    private Integer type;

    @TableField("REQUIRED")
    @ApiModelProperty("是否必填")
    @Excel(name = "是否必填")
    private Integer required;

    @TableField("DEFAULT_VALUE")
    @ApiModelProperty("默认值")
    @Excel(name = "默认值")
    private String defaultValue;

    @TableField("VALIDATE_TYPE")
    @ApiModelProperty("校验类型")
    @Excel(name = "校验类型")
    private Integer validateType;

    @TableField("EXPRESSION")
    @ApiModelProperty("校验表达式")
    @Excel(name = "校验表达式")
    private String expression;

    @TableField("ERROR")
    @ApiModelProperty("校验说明")
    @Excel(name = "校验说明")
    private String error;

    @TableField("DESCRIPTION")
    @ApiModelProperty("说明")
    @Excel(name = "参数说明")
    private String description;
}
