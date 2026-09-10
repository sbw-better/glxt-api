package com.citics.glxtapi.web.constants;

/**
 * 系统常量类
 */
public class Constants {

    /**
     * 空值
     */
    public static final String EMPTY = "";

    /**
     * 脚本中Context的变量名
     */
    public static final String VAR_NAME_MODULE_CONTEXT = "context";

    /**
     * 表达式验证
     */
    public static final String VALIDATE_TYPE_EXPRESSION = "expression";

    /**
     * 正则验证
     */
    public static final String VALIDATE_TYPE_PATTERN = "pattern";

    /**
     * 表达式验证中变量的默认名称
     */
    public static final String EXPRESSION_DEFAULT_VAR_NAME = "value";

    /**
     * 脚本中session的变量名
     */
    public static final String VAR_NAME_SESSION = "session";

    /**
     * 脚本中cookie的变量名
     */
    public static final String VAR_NAME_COOKIE = "cookie";

    /**
     * 脚本中路径变量的变量名
     */
    public static final String VAR_NAME_PATH_VARIABLE = "path";

    /**
     * 脚本中header的变量名
     */
    public static final String VAR_NAME_HEADER = "header";

    /**
     * 脚本中RequestBody的变量名
     */
    public static final String VAR_NAME_REQUEST_BODY = "body";

    /**
     * 脚本中Http的变量名
     */
    public static final String VAR_NAME_MODULE_HTTP = "http";

    /**
     * 脚本中DB的变量名
     */
    public static final String VAR_NAME_MODULE_DB = "db";

    /**
     * 脚本中Request的变量名
     */
    public static final String VAR_NAME_MODULE_REQUEST = "request";

    /**
     * 脚本中Response的变量名
     */
    public static final String VAR_NAME_MODULE_RESPONSE = "response";

    /**
     * 脚本中Word的变量名
     */
    public static final String VAR_NAME_MODULE_WORD = "word";

    /**
     * 脚本中日志的变量名
     */
    public static final String VAR_NAME_MODULE_LOG = "log";

    /**
     * 脚本中插件的变量名
     */
    public static final String VAR_NAME_MODULE_PLUGIN = "plugin";

    /**
     * 脚本中环境变量的变量名
     */
    public static final String VAR_NAME_MODULE_ENV = "env";

    /**
     * 脚本中Sweet的变量名
     */
    public static final String VAR_NAME_MODULE_SWEET = "sweet";

    public static final String HEADER_REQUEST_CLIENT_ID = "Sweet-Request-Client-Id";

    public static final String UPLOAD_MODE_FULL = "full";

    /**
     * 执行成功的code值
     */
    public static int RESPONSE_CODE_SUCCESS = 0;

    /**
     * 执行成功的message值
     */
    public static final String RESPONSE_MESSAGE_SUCCESS = "success";

    /**
     * 执行出现异常的code值
     */
    public static int RESPONSE_CODE_EXCEPTION = -1;

    /**
     * 参数验证未通过的code值
     */
    public static int RESPONSE_CODE_INVALID = -404;

    /**
     * 接口类型 1|API
     */
    public static int INTERFACE_TYPE_API = 1;
    public static int INTERFACE_TYPE_PROCEDURE = 2;

    /**
     * 是否 0|否  1|是
     */
    public static Integer WHETHER_YES = 1;
    public static Integer WHETHER_NO = 0;

    /**
     * 参数类型 1|字符串  2|整型  3|浮点型  4|日期型  5|数组
     */
    public static int FILED_TYPE_STRING = 1;
    public static int FILED_TYPE_INT = 2;
    public static int FILED_TYPE_FLOAT = 3;
    public static int FILED_TYPE_DATE = 4;
    public static int FILED_TYPE_LIST = 5;

    /**
     * 存储过程参数方向 1|IN 2|OUT 3|INOUT
     */
    public static int PROCEDURE_PARAM_DIRECTION_IN = 1;
    public static int PROCEDURE_PARAM_DIRECTION_OUT = 2;
    public static int PROCEDURE_PARAM_DIRECTION_INOUT = 3;

    /**
     * 存储过程JDBC类型
     */
    public static String PROCEDURE_JDBC_TYPE_VARCHAR = "VARCHAR";
    public static String PROCEDURE_JDBC_TYPE_INTEGER = "INTEGER";
    public static String PROCEDURE_JDBC_TYPE_BIGINT = "BIGINT";
    public static String PROCEDURE_JDBC_TYPE_DECIMAL = "DECIMAL";
    public static String PROCEDURE_JDBC_TYPE_DATE = "DATE";
    public static String PROCEDURE_JDBC_TYPE_TIMESTAMP = "TIMESTAMP";
    public static String PROCEDURE_JDBC_TYPE_CLOB = "CLOB";
    public static String PROCEDURE_JDBC_TYPE_CURSOR = "CURSOR";

    /**
     * 存储过程 JDBC 类型的前端下拉值。配置保存时会转换为上面的字符串类型，
     * 因此数据库、Excel 和执行层均不依赖这些数字。
     */
    public static int PROCEDURE_JDBC_TYPE_VALUE_VARCHAR = 1;
    public static int PROCEDURE_JDBC_TYPE_VALUE_INTEGER = 2;
    public static int PROCEDURE_JDBC_TYPE_VALUE_BIGINT = 3;
    public static int PROCEDURE_JDBC_TYPE_VALUE_DECIMAL = 4;
    public static int PROCEDURE_JDBC_TYPE_VALUE_DATE = 5;
    public static int PROCEDURE_JDBC_TYPE_VALUE_TIMESTAMP = 6;
    public static int PROCEDURE_JDBC_TYPE_VALUE_CLOB = 7;
    public static int PROCEDURE_JDBC_TYPE_VALUE_CURSOR = 8;

    /**
     * 参数校验类型  1|不验证  2|表达式验证  3|正则验证
     */
    public static int FILED_CHECK_TYPE_NO = 1;
    public static int FILED_CHECK_TYPE_EXPRESSION = 2;
    public static int FILED_CHECK_TYPE_PATTERN = 3;

    /**
     * SQL执行结果  1|成功  0|失败且不显示错误信息  -1|失败
     */
    public static int SQL_EXECUTE_SUCCESS = 1;
    public static int SQL_EXECUTE_FAIL_PRIVATE = 0;
    public static int SQL_EXECUTE_FAIL = -1;

    /**
     * 接口默认数据源ID
     */
    public static Long API_DEFAULT_DATA_SOURCE_ID = 0L;

    /**
     * 接口管理人ID、产品ID列表、产品代码列表
     */
    public static String API_PARAM_MANAGER_ID = "managerField";
    public static String API_PARAM_PRODUCT_ID_LIST = "fundIdsField";
    public static String API_PARAM_PRODUCT_CODE_LIST = "fundCodesField";

    /**
     * 特殊租户
     */
    // 管理系统管理员租户
    public static String TENANT_ADMIN = "admin";
    // 全量查看租户
    public static String TENANT_ALL = "all";
    // 登录用户没有任何租户时的传入
    public static String TENANT_NONE = "none";

    /**
     * 导入导出时的sheet名称、文件名
     */
    // 接口配置记录导出excel表名
    public static String EXCEL_INTERFACE = "接口配置记录导出";
    // 接口配置记录
    public static String SHEET_INTERFACE = "接口配置记录";
    // 接口参数记录
    public static String SHEET_INTERFACE_PARAM = "接口参数记录";

    /**
     * pulsar消息队列消息供哪个系统或业务使用的topic标志
     */
    public static String PULSAR_PRODUCE_SYS_TOPIC = "sys_topic";

    /**
     * 接口字段返回模式  1|默认模式(全小写)  2|按别名实际情况
     */
    public static Integer API_FIELD_BACK_MODE_DEFAULT = 1;
    public static Integer API_FIELD_BACK_MODE_ALIAS_NAME = 2;

}
