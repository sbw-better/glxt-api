package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Dict;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@TableName("API_SQL_TENANT_INTERFACE")
@KeySequence(value = "SEQ_API_SQL_TENANT_INTERFACE")
@ApiModel(value="租户权限接口关系权限", description="")
public class TenantInterface extends Model<TenantInterface> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
    private Long id;

    @JsonIgnore
    public boolean isNew(){
        if(null == id || -1L == id) {
            return true;
        }
        return false;
    }

    @TableField("AUTH_ID")
    private Long authId;

    @Dict(objCode = "table=API_SQL_INTERFACE,display=NAME,key=ID", type = "single", convertMode = "replace")
    @TableField("API_ID")
    private Long apiId;

}
