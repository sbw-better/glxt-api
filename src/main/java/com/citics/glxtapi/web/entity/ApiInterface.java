package com.citics.glxtapi.web.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.citics.glxtapi.common.annotation.Dict;
import com.citics.glxtapi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@TableName("API_SQL_INTERFACE")
@NoArgsConstructor
@KeySequence(value = "SEQ_API_SQL_INTERFACE")
public class ApiInterface extends Model<ApiInterface> {

    private static final long serialVersionUID = 9138351484662216546L;

    @TableId("ID")
    @ApiModelProperty("主键")
    @Excel(name = "接口ID")
    private Long id;

    @JsonIgnore
    public boolean isNew(){
        if(null == id || -1L == id){
            return true;
        }
        return false;
    }

    @TableField("TENANT")
    @ApiModelProperty("租户")
    @Excel(name = "接口租户")
    private String tenant;

    @TableField("NAME")
    @ApiModelProperty("名称")
    @Excel(name = "接口名称")
    private String name;

    @TableField("TYPE")
    @ApiModelProperty("类型")
    @Excel(name = "接口类型")
    private Integer type;

    @TableField("CODE")
    @ApiModelProperty("代码")
    @Excel(name = "接口代码")
    private String code;

    @TableField("DESCRIPTION")
    @ApiModelProperty("描述")
    @Excel(name = "接口描述")
    private String description;

    @TableField("ORDER_NO")
    @ApiModelProperty("排序号")
    @Excel(name = "接口排序号")
    private Integer orderNo;

    @TableField("SELECT_PARAM")
    @ApiModelProperty("SELECT参数")
    @Excel(name = "SELECT参数")
    private String selectParam;

    @TableField("ZDFHMS")
    @ApiModelProperty("字段返回模式")
    @Excel(name = "字段返回模式")
    private Integer fieldBackMode;

    @TableField("FROM_PARAM")
    @ApiModelProperty("FROM参数")
    @Excel(name = "FROM参数")
    private String fromParam;

    @TableField("WHERE_PARAM_FIXED")
    @ApiModelProperty("固定WHERE参数")
    @Excel(name = "固定WHERE参数")
    private String whereParamFixed;

    @TableField("WHERE_PARAM_CHANGE")
    @ApiModelProperty("可变WHERE参数")
    @Excel(name = "可变WHERE参数")
    private String whereParamChange;

    @TableField("GROUP_PARAM")
    @ApiModelProperty("GROUP参数")
    @Excel(name = "GROUP参数")
    private String groupParam;

    @TableField("ORDER_PARAM")
    @ApiModelProperty("ORDER参数")
    @Excel(name = "ORDER参数")
    private String orderParam;

    @TableField("PAGE")
    @ApiModelProperty("是否分页")
    @Excel(name = "是否分页")
    private Integer page;

    @Dict(objCode = "table=API_SQL_CONNECTION,display=CODE,key=ID", type = "single", convertMode = "replace")
    @ApiModelProperty("数据源")
    @TableField("CONNECTION_ID")
    @Excel(name = "数据源ID")
    private Long connectionId;

    @TableField("MANAGER_FIELD")
    @ApiModelProperty("管理人字段")
    @Excel(name = "管理人字段")
    private String managerField;

    @TableField("FUND_IDS_FIELD")
    @ApiModelProperty("产品ID字段")
    @Excel(name = "产品ID字段")
    private String fundIdsField;

    @TableField("FUND_CODES_FIELD")
    @ApiModelProperty("产品代码字段")
    private String fundCodesField;

    @TableField("PREVIEW_SQL")
    @ApiModelProperty("预览SQL")
    @Excel(name = "预览SQL")
    private String previewSql;

    @TableField("CREATE_TIME")
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "创建时间", dateFormat = "yyyy/MM/dd HH:mm:ss")
    private Date createTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("CREATE_BY")
    @ApiModelProperty("创建人")
    @Excel(name = "创建人ID")
    private Long createBy;

    @TableField("UPDATE_TIME")
    @ApiModelProperty("修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "修改时间", dateFormat = "yyyy/MM/dd HH:mm:ss")
    private Date updateTime;

    @Dict(objCode = "table=V_TGCOMMON_SYSUSER,display=USER_NAME,key=USER_ID", type = "single", convertMode = "replace")
    @TableField("UPDATE_BY")
    @ApiModelProperty("修改人")
    @Excel(name = "修改人ID")
    private Long updateBy;
}
