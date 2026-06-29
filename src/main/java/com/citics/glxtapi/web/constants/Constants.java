package com.citics.glxtapi.web.constants;

public class Constants {
    /**
     * 接口默认数据源ID
     */
    public static Long API_DEFAULT_DATA_SOURCE_ID = 0L;
    /**
     * 是否 0|否  1|是
     */
    public static Integer WHETHER_YES = 1;
    public static Integer WHETHER_NO = 0;

    /**
     * SQL执行结果
     * 1|成功  0|失败且不显示错误信息   -1|失败
     **/
    public static int SQL_EXECUTE_SUCCESS = 1;
    public static int SQL_EXECUTE_FAIL_PRIVATE = 0;
    public static int SQL_EXECUTE_FAIL = -1;

    public static Integer API_FIELD_BACK_MODE_ALIAS_NAME = 2;

    /**
     * 脚本中DB的变量名
     */
    public static final String VAR_NAME_MODULE_DB = "db";

    /**
     * 接口管理人ID、产品ID列表、产品代码列表
     **/
    public static String API_PARAM_MANAGER_ID = "managerField";

    public static String API_PARAM_PRODUCT_ID_LIST = "fundIdsField";

    public static String API_PARAM_PRODUCT_CODE_LIST = "fundCodesField";

    /**
     * 参数类型  1|字符串  2|整型  3|浮点型  4|日期型  5|数组
     **/
    public static int FILED_TYPE_STRING = 1;

    public static int FILED_TYPE_INT = 2;

    public static int FILED_TYPE_FLOAT = 3;

    public static int FILED_TYPE_DATE = 4;

    public static int FILED_TYPE_LIST = 5;

    /**
     * 参数校验类型  1|不验证  2|表达式验证  3|正则验证
     **/
    public static int FILED_CHECK_TYPE_NO = 1;

    public static int FILED_CHECK_TYPE_EXPRESSION = 2;

    public static int FILED_CHECK_TYPE_PATTERN = 3;
}
