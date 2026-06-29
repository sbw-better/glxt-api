package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Dict;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@TableName("API_SQL_CONNECTION")
@KeySequence(value = "SEQ_API_SQL_CONNECTION")
@ApiModel(value = "连接管理", description = "")
public class Connection extends Model<Connection> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
    @ApiModelProperty("主键")
    private Long id;

    @JsonIgnore
    public boolean isNew() {
        if (null == id || -1L == id) {
            return true;
        }
        return false;
    }

    @TableField("NAME")
    @ApiModelProperty(value = "名称")
    private String name;

    @TableField("TYPE")
    @ApiModelProperty(value = "类型")
    private String type;

    @TableField("CONFIG")
    @ApiModelProperty(value = "连接配置(JSON格式)")
    private String config;

    @TableField("EXTEND_CONFIG")
    @ApiModelProperty(value = "扩展配置(JSON格式)")
    private String extendConfig;

    @TableField("TIMEOUT")
    @ApiModelProperty(value = "请求超时(单位：秒，0/-1代表不超时)")
    private int timeout;

    @TableField("CODE")
    @ApiModelProperty(value = "编码")
    private String code;

    @TableField("TENANT")
    @ApiModelProperty(value = "租户编码")
    private String tenant;

    @TableField("CREATE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("CREATE_BY")
    @ApiModelProperty(value = "创建人")
    private Long createBy;

    @TableField("UPDATE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("UPDATE_BY")
    @ApiModelProperty(value = "更新人")
    private Long updateBy;
}
