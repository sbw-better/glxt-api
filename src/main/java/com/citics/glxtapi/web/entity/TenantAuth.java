package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Dict;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@TableName("API_SQL_TENANT_AUTH")
@KeySequence(value = "SEQ_API_SQL_TENANT_AUTH")
@ApiModel(value = "租户分组权限", description = "")
public class TenantAuth extends Model<TenantAuth> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
    private Long id;

    @JsonIgnore
    public boolean isNew() {
        if (null == id || -1L == id) {
            return true;
        }
        return false;
    }

    @TableField("TENANT")
    private String tenant;

    @TableField("NAME")
    private String name;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("TOKEN")
    private String token;

    @TableField("IP_CONFIG")
    @ApiModelProperty("IP配置 [ {\"ip\":\"172.22.161.97\"},{\"ip\":\"172.22.161.204\"} ]")
    private String ipConfig;

    @TableField("CREATE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("CREATE_BY")
    private Long createBy;

    @TableField("UPDATE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("UPDATE_BY")
    private Long updateBy;
}
