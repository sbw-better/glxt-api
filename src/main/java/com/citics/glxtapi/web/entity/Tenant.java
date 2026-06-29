package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@TableName("API_SQL_TENANT")
@KeySequence(value = "SEQ_API_SQL_TENANT")
@ApiModel(value = "租户管理", description = "")
public class Tenant extends Model<Tenant> {

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
    @ApiModelProperty("租户名称")
    private String name;

    @TableField("TENANT")
    @ApiModelProperty("租户代码")
    private String tenant;

    @TableField("CREATE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
