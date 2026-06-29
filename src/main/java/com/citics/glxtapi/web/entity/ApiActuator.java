package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Dict;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@TableName("API_SQL_ACTUATOR")
@NoArgsConstructor
@KeySequence(value = "SEQ_API_SQL_INTERFACE_ACTUATOR")
@ApiModel(value = "接口执行记录", description = "")
public class ApiActuator extends Model<ApiActuator> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
    @ApiModelProperty("主键")
    private Long id;

    @TableField("TENANT")
    @ApiModelProperty("租户")
    private String tenant;

    @TableField("SYSTEM_CODE")
    @ApiModelProperty("使用系统")
    private String systemCode;

    @Dict(objCode = "table=API_SQL_INTERFACE,display=NAME,key=ID", type = "single", convertMode = "replace")
    @TableField("API_ID")
    @ApiModelProperty("接口ID")
    private Long apiId;

    @TableField("API_CODE")
    @ApiModelProperty("接口编码")
    private String apiCode;

    @TableField("INTER_PARAM")
    @ApiModelProperty("入参")
    private String interParam;

    @TableField("INTER_IP")
    @ApiModelProperty("输入IP")
    private String interIp;

    @TableField("EXECUTE_TIME")
    @ApiModelProperty("执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date executeTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("EXECUTE_BY")
    @ApiModelProperty("执行人")
    private Long executeBy;

    @TableField("EXECUTE_RESULT")
    @ApiModelProperty("执行结果")
    private Integer executeResult;

    @TableField("EXECUTE_RESULT_DETAIL")
    @ApiModelProperty("执行结果详情")
    private String executeResultDetail;

    @TableField("EXECUTE_SQL")
    @ApiModelProperty("执行SQL")
    private String executeSql;

    @TableField("EXECUTE_CONSUME")
    @ApiModelProperty("执行耗时")
    private String executeConsume;

    @TableField("RESULT_COUNT")
    @ApiModelProperty("查询结果数量")
    private Integer resultCount;
}