package com.citics.glxtapi.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.common.factory.PageFactory;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.utils.date.DateUtils;
import com.citics.glxtapi.common.utils.http.IpUtil;
import com.citics.glxtapi.common.utils.page.PageUtils;
import com.citics.glxtapi.plugin.db.support.DdConstants;
import com.citics.glxtapi.plugin.sql.DbModule;
import com.citics.glxtapi.web.entity.ApiActuator;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.dto.ApiActuatorDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.exception.APIException;
import com.citics.glxtapi.web.mapper.ApiActuatorMapper;
import com.citics.glxtapi.web.service.*;
import com.citics.glxtapi.web.support.SqlContextHolder;
import com.citics.glxtapi.web.support.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;
import static com.citics.glxtapi.web.constants.Constants.*;


@Service
@Slf4j
public class ApiActuatorServiceImpl extends ServiceImpl<ApiActuatorMapper, ApiActuator> implements ApiActuatorService {

    @Resource
    private ApiService apiService;
    @Resource
    private TenantService tenantService;
    @Resource
    private ApiParamService apiParamService;
    @Resource
    private TenantAuthService tenantAuthService;
    @Resource
    private ConnectionService connectionService;
    @Resource
    private DbModule dbModule;

    @Value("${common.execute_test_max_count}")
    private int executeTestMaxCount;

    @Value("${common.sql_result_page_max_row}")
    private int sqlResultPageMaxRow;

    @Override
    public Object execute(String apiActuatorInfo, HttpServletRequest req) {
        // 解析接口信息
        JSONObject apiActuatorInfoJson = JSON.parseObject(apiActuatorInfo);
        String tenant = apiActuatorInfoJson.getString("tenant");
        String apiCode = apiActuatorInfoJson.getString("apiCode");
        Boolean fieldAuth = apiActuatorInfoJson.getBoolean("fieldAuth");
        Boolean pageNeed = apiActuatorInfoJson.getBoolean("pageNeed");
        String ip = IpUtil.getClientIp(req);

        // 校验、入参处理
        isFalse(StringUtils.isEmpty(apiCode), "接口代码为空，请核对！");
        ApiInterfaceVO apiInterfaceVO = this.apiService.getByApi(tenant, apiCode);
        Map<String, Object> paramsMap = infoCheck(apiInterfaceVO, apiActuatorInfoJson, ip, fieldAuth, pageNeed);

        // 拼接SQL
        String sql = concatSql(apiInterfaceVO, paramsMap);
        isFalse(StringUtils.isEmpty(sql), "接口SQL语句拼接出错，请联系管理员！");
        // log.info("sql语句执行，接口={},sql={},param={}", tenant + "-" + apiCode, sql, paramsMap);

        // 查询SQL
        Object sqlRes = null;
        try {
            if (apiInterfaceVO.getConnectionId().equals(API_DEFAULT_DATA_SOURCE_ID)) {
                this.tenantService.clearDs();
            } else {
                this.tenantService.setDs(connectionService.getById(apiInterfaceVO.getConnectionId()).getCode() + "_" + DdConstants.MASTER);
            }

            if ((pageNeed != null && pageNeed.equals(true)) || (null == pageNeed && WHETHER_YES.equals(apiInterfaceVO.getPage()))) {
                // 使用自定义分页结果
                // sqlRes = dbModule.page1(sql, apiActuatorInfoJson.getInteger("pageNum"), apiActuatorInfoJson.getInteger("pageSize"), paramsMap, true);
                // 使用pageHelper分页结果
                sqlRes = dbModule.page2(sql, apiActuatorInfoJson.getInteger("pageNum"), apiActuatorInfoJson.getInteger("pageSize"), paramsMap, true, apiInterfaceVO.getFieldBackMode());
            } else {
                sqlRes = dbModule.select(sql, paramsMap, true, apiInterfaceVO.getFieldBackMode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new APIException("SQL执行错误！" + e.getMessage());
        } finally {
            this.tenantService.clearDs();
        }

        // 返回执行结果
        return sqlRes;
    }

    @Override
    public PageResult page(ApiActuatorDTO dto) {
        QueryWrapper<ApiActuator> queryWrapper = new QueryWrapper<>();
        // 设置租户信息
        String tenant = this.tenantService.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(ApiActuator::getTenant, tenant);
        }
        // 接口ID匹配
        if (null != dto.getApiId()) {
            queryWrapper.lambda().eq(ApiActuator::getApiId, dto.getApiId());
        }
        // 接口代码匹配，模糊匹配且不区分大小写
        if (StringUtils.isNotEmpty(dto.getApiCode())) {
            queryWrapper.lambda().apply("UPPER(API_CODE) LIKE UPPER({0})", "%" + dto.getApiCode() + "%");
        }
        // 系统代码匹配
        if (StringUtils.isNotEmpty(dto.getSystemCode())) {
            queryWrapper.lambda().apply("UPPER(SYSTEM_CODE) LIKE UPPER({0})", "%" + dto.getSystemCode() + "%");
        }
        // 输入IP匹配
        if (StringUtils.isNotEmpty(dto.getInterIp())) {
            queryWrapper.lambda().eq(ApiActuator::getInterIp, dto.getInterIp());
        }
        // 执行结果匹配
        if (null != dto.getExecuteResult()) {
            queryWrapper.lambda().eq(ApiActuator::getExecuteResult, dto.getExecuteResult());
        }
        // 执行人匹配
        if (null != dto.getExecuteBy()) {
            queryWrapper.lambda().eq(ApiActuator::getExecuteBy, dto.getExecuteBy());
        }
        // 执行开始时间匹配
        if (null != dto.getExecuteTimeStart()) {
            queryWrapper.lambda().apply("TO_CHAR(EXECUTE_TIME,'YYYYMMDD') >= '" + DateUtils.parseDateToStr(DateUtils.YYYYMMDD, dto.getExecuteTimeStart()) + "'");
        }
        // 执行结束时间匹配
        if (null != dto.getExecuteTimeEnd()) {
            queryWrapper.lambda().apply("TO_CHAR(EXECUTE_TIME,'YYYYMMDD') <= '" + DateUtils.parseDateToStr(DateUtils.YYYYMMDD, dto.getExecuteTimeEnd()) + "'");
        }

        // 入参模糊匹配
        queryWrapper.lambda().like(StringUtils.isNotEmpty(dto.getInterParam()), ApiActuator::getInterParam, dto.getInterParam());
        queryWrapper.orderByDesc("id");

        return PageUtils.getPageResult(this.page(PageFactory.jsonPage(dto.getPageNum(), dto.getPageSize()), queryWrapper));
    }

    @Override
    public void insertAfterExecute(String apiActuatorInfo, HttpServletRequest req, boolean success, Object sqlRes, long consumeTime) {
        // 解析信息
        JSONObject apiActuatorInfoJson = JSON.parseObject(apiActuatorInfo);
        String tenant = apiActuatorInfoJson.getString("tenant");
        String apiCode = apiActuatorInfoJson.getString("apiCode");
        String ip = IpUtil.getClientIp(req);
        ApiInterfaceVO apiInterfaceVO = this.apiService.getByApi(tenant, apiCode);

        ApiActuator actuator = new ApiActuator();
        if (sqlRes == null) {
            actuator.setExecuteResultDetail(null);
        } else {
            actuator.setExecuteResultDetail(JSONObject.toJSONString(sqlRes));
        }

        // 插入执行记录
        actuator.setTenant(apiInterfaceVO.getTenant());
        actuator.setSystemCode(apiActuatorInfoJson.getString("systemCode"));
        actuator.setApiId(apiInterfaceVO.getId());
        actuator.setApiCode(apiCode);
        actuator.setInterParam(apiActuatorInfo);
        actuator.setInterIp(ip);
        actuator.setExecuteTime(new Date());
        actuator.setExecuteBy(UserContextHolder.getUserId());
        actuator.setExecuteResult(success ? SQL_EXECUTE_SUCCESS : SQL_EXECUTE_FAIL);
        actuator.setExecuteSql(SqlContextHolder.getSql());
        actuator.setResultCount(SqlContextHolder.getSqlCount());
        actuator.setExecuteConsume(String.valueOf(consumeTime));
        this.save(actuator);
    }

    @Override
    public boolean executeCheck(ApiActuatorDTO dto) {
        // 执行器测试按钮，先判断sql结果数量是不是超过20
        QueryWrapper<ApiActuator> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ApiActuator::getId, dto.getId());
        ApiActuator actuator = this.getOne(wrapper);
        isFalse(actuator.getResultCount() > executeTestMaxCount, "此条SQL执行结果数量较多，请不要在前端测试！");
        return true;
    }

    /**
     * 对接口调用传入的参数进行校验
     * @param apiInterfaceVO 接口信息
     * @param apiActuatorInfoJson 接口调用传递信息
     * @param ip 调用方ip
     * @return
     */
    public Map<String, Object> infoCheck(ApiInterfaceVO apiInterfaceVO, JSONObject apiActuatorInfoJson, String ip, Boolean fieldAuth, Boolean pageNeed) {
        Map<String, Object> paramsMap = new HashMap<>();

        // 分页、权限、接口代码等信息校验
        isFalse(null == apiInterfaceVO.getPage(), "接口未配置是否分页，请联系管理员！");
        Integer pageNum, pageSize;
        pageNum = apiActuatorInfoJson.getInteger("pageNum");
        pageSize = apiActuatorInfoJson.getInteger("pageSize");
        if ((pageNeed != null && pageNeed.equals(true)) || (null == pageNeed && WHETHER_YES.equals(apiInterfaceVO.getPage()))) {
            isFalse(pageNum == null || pageSize == null, "是否分页配置为是,或pageNeed入参为true时,pageNum和pageSize均不可为空，请核对！");
            isFalse((pageNum != null && pageNum <= 0) || (pageSize != null && pageSize <= 0), "pageNum和pageSize均需为大于0的整数，请核对！");
            isFalse(pageSize != null && pageSize > sqlResultPageMaxRow, "分页记录pageSize不能超过" + sqlResultPageMaxRow + "，请核对！");
        }
        String token = apiActuatorInfoJson.getString("token");
        String systemCode = apiActuatorInfoJson.getString("systemCode");
        isFalse(!tenantAuthService.hasExecutePermission(apiInterfaceVO.getId(), token, ip), "请检查接口权限！");
        isFalse(StringUtils.isEmpty(systemCode), "系统代码不可为空，请核对！");

        // 校验参数基本信息校验、入参处理
        boolean apiHaveRequiredParam = this.apiParamService.apiHaveRequiredParam(apiInterfaceVO.getId());
        JSONObject paramsJson = apiActuatorInfoJson.getJSONObject("params");
        // 1、接口存在必填的参数配置，且调用时未传param参数的情况
        isFalse(apiHaveRequiredParam && null == paramsJson, "接口存在必填参数的配置，但调用时未传param参数，请核对！");

        // 2.0、单独校验三个鉴权字段
        managerProductCheck(apiInterfaceVO, paramsJson, paramsMap, fieldAuth);

        // 2、接口存在必填的参数配置，且调用时有传param参数的情况
        // 3、接口不存在必填的参数配置，且调用时有传param参数的情况
        if (null != paramsJson) {
            for (Map.Entry<String, Object> entry : paramsJson.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals(API_PARAM_MANAGER_ID) || key.equals(API_PARAM_PRODUCT_ID_LIST) || key.equals(API_PARAM_PRODUCT_CODE_LIST)) {
                    continue;
                }
                boolean isHaveParam = this.apiParamService.apiHaveThisParam(apiInterfaceVO.getId(), key);
                isFalse(!isHaveParam, "接口" + apiInterfaceVO.getCode() + "配置中不存在参数" + key + "，请检查或核对大小写！");
                paramsMap.put(key, value);
            }

            boolean isFull = this.apiParamService.requiredParamIsFull(apiInterfaceVO.getId(), new ArrayList<>(paramsMap.keySet()));
            isFalse(!isFull, "接口" + apiInterfaceVO.getCode() + "的必传参数不全，请核对！");
            List<ApiParam> paramDefaultList = this.apiParamService.notRequiredParamList(apiInterfaceVO.getId(), new ArrayList<>(paramsMap.keySet()));
            for (ApiParam ap : paramDefaultList) {
                paramsMap.put(ap.getCode(), ap.getDefaultValue());
            }
        } else {
            // 4、接口不存在必填的参数配置，且调用时未传param参数的情况
            List<ApiParam> paramDefaultList = this.apiParamService.notRequiredParamList(apiInterfaceVO.getId(), new ArrayList<>());
            for (ApiParam ap : paramDefaultList) {
                paramsMap.put(ap.getCode(), ap.getDefaultValue());
            }
        }

        // 参数类型校验、正则校验、表达式校验
        List<ApiParam> paramList = this.apiParamService.listByCodeList(apiInterfaceVO.getId(), new ArrayList<>(paramsMap.keySet()));
        String validatedMsg = paramListValidate(paramList, paramsMap, apiInterfaceVO);
        isFalse(StringUtils.isNotEmpty(validatedMsg), validatedMsg);

        return paramsMap;
    }

    public String paramListValidate(List<ApiParam> paramList, Map<String, Object> paramsMap, ApiInterfaceVO apiInterfaceVO) {
        // 先字段类型校验、正则校验
        for (ApiParam p : paramList) {
            int type = p.getType();
            Object val = paramsMap.get(p.getCode());
            if (type == FILED_TYPE_STRING) {
                isFalse(!(val instanceof String), "参数" + p.getCode() + "应为字符串类型，请核对！");
            }
            if (type == FILED_TYPE_INT) {
                boolean intCheck = false;
                if (val instanceof Integer || val instanceof Long) {
                    intCheck = true;
                } else if (val instanceof String) {
                    try {
                        intCheck = true;
                        Long.valueOf((String) val);
                    } catch (Exception e) {
                        intCheck = false;
                    }
                }
                isFalse(!intCheck, "参数" + p.getCode() + "应为整型，请核对！");
                paramsMap.put(p.getCode(), Long.valueOf(String.valueOf(val)));
            }
            if (type == FILED_TYPE_FLOAT) {
                boolean floatCheck = false;
                if (val instanceof Float || val instanceof Double || val instanceof BigDecimal) {
                    floatCheck = true;
                } else if (val instanceof String) {
                    try {
                        floatCheck = true;
                        Double.valueOf((String) val);
                    } catch (Exception e) {
                        floatCheck = false;
                    }
                }
                isFalse(!floatCheck, "参数" + p.getCode() + "应为浮点型，请核对！");
            }
            if (type == FILED_TYPE_DATE) {
                isFalse(!(val instanceof String), "参数" + p.getCode() + "应为日期型(yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss)，请核对调用传参或接口配置的参数默认值！");
                Matcher FieldMc = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$").matcher((String) val);
                if (!FieldMc.find()) {
                    FieldMc = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$").matcher((String) val);
                    isFalse(!FieldMc.find(), "参数" + p.getCode() + "应为日期型(yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss)，请核对调用传参或接口配置的参数默认值！");
                }
            }
            if (type == FILED_TYPE_LIST) {
                isFalse(!(val instanceof List), "参数" + p.getCode() + "应为list类型，请核对！");
                List<?> valList = (List<?>) val;
                isFalse(valList != null && valList.size() == 0, "参数" + p.getCode() + "为list类型，list中不允许空元素，请核对！");
                paramsMap.put(p.getCode(), StringUtils.join((List<?>) val, ";"));
            }

            if (p.getValidateType() == FILED_CHECK_TYPE_PATTERN) {
                String expression = p.getExpression();
                String error = p.getError();
                String regex = "#\\{(.+?)\\}";
                Pattern FieldPt = Pattern.compile(expression);
                Matcher FieldMc = FieldPt.matcher(String.valueOf(paramsMap.get(p.getCode())));
                if (!FieldMc.find()) {
                    Pattern errorPt = Pattern.compile(regex);
                    Matcher errorMc = errorPt.matcher(error);
                    while (errorMc.find()) {
                        isFalse(!paramsMap.containsKey(errorMc.group(2)), "参数" + p.getCode() + "的正则验证说明中的" + errorMc.group(1) + "不存在，请核对！");
                        error = error.replace(errorMc.group(1), String.valueOf(paramsMap.get(errorMc.group(2))));
                    }
                    return error;
                }
            }
        }

        // 再表达式验证
        for (ApiParam p : paramList) {
            if (p.getValidateType() == FILED_CHECK_TYPE_EXPRESSION) {
                String expression = p.getExpression();
                String error = p.getError();
                String regex = "#\\{(.+?)\\}";
                Pattern ExpressionPt = Pattern.compile(regex);
                Matcher ExpressionMc = ExpressionPt.matcher(expression);
                Map<String, Object> expressionMap = new HashMap<>();
                while (ExpressionMc.find()) {
                    isFalse(!paramsMap.containsKey(ExpressionMc.group(2)), "参数" + p.getCode() + "的表达式验证表达式中的" + ExpressionMc.group(1) + "不存在，请核对！");
                    expressionMap.put(ExpressionMc.group(2), paramsMap.get(ExpressionMc.group(2)));
                }

                Integer sqlRes = null;
                try {
                    if (apiInterfaceVO.getConnectionId().equals(API_DEFAULT_DATA_SOURCE_ID)) {
                        this.tenantService.clearDs();
                    } else {
                        this.tenantService.setDs(connectionService.getById(apiInterfaceVO.getConnectionId()).getCode() + "_" + DdConstants.MASTER);
                    }
                    sqlRes = dbModule.selectInt(expression, expressionMap, false, null);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    this.tenantService.clearDs();
                }

                if (null != sqlRes && sqlRes < 1) {
                    Pattern errorPt = Pattern.compile(regex);
                    Matcher errorMc = errorPt.matcher(error);
                    while (errorMc.find()) {
                        isFalse(!paramsMap.containsKey(errorMc.group(2)), "参数" + p.getCode() + "的表达式验证说明中的" + errorMc.group(1) + "不存在，请核对！");
                        error = error.replace(errorMc.group(1), String.valueOf(paramsMap.get(errorMc.group(2))));
                    }
                    return error;
                }
            }
        }
        return "";
    }

    public void managerProductCheck(ApiInterfaceVO apiInterfaceVO, JSONObject paramsJson, Map<String, Object> paramsMap, Boolean fieldAuth) {
        boolean paramsJsonNull = paramsJson == null;
        Set<String> paramKeys = new HashSet<>();
        if (!paramsJsonNull) {
            paramKeys = paramsJson.keySet();
        }

        //fieldAuth=true：鉴权主动权在接口配置端，若鉴权字段配置则必传鉴权参数，不传报错；若鉴权字段未配置，则鉴权参数传不传均可
        if (fieldAuth != null && fieldAuth.equals(true)) {
            if (StringUtils.isNotEmpty(apiInterfaceVO.getManagerField())) {
                isFalse(paramsJsonNull, "接口需要对管理人ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_MANAGER_ID + "字段");
                isFalse(!paramKeys.contains(API_PARAM_MANAGER_ID), "接口需要对管理人ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_MANAGER_ID + "字段");
                Object managerId = paramsJson.get(API_PARAM_MANAGER_ID);
                boolean intCheck = false;
                if (managerId instanceof Integer || managerId instanceof Long) {
                    intCheck = true;
                } else if (managerId instanceof String) {
                    try {
                        intCheck = true;
                        Long.valueOf((String) managerId);
                    } catch (Exception e) {
                        intCheck = false;
                    }
                }
                isFalse(!intCheck, "参数" + API_PARAM_MANAGER_ID + "应为整型，请核对！");
                paramsMap.put(API_PARAM_MANAGER_ID, managerId);
            }

            // 20251020沟通新逻辑，当鉴权开关打开时，产品维度鉴权二选一即可
            // 只配置了产品ID鉴权
            if (StringUtils.isNotEmpty(apiInterfaceVO.getFundIdsField()) && StringUtils.isEmpty(apiInterfaceVO.getFundCodesField())) {
                isFalse(paramsJsonNull, "接口需要对产品ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_ID_LIST + "字段");
                isFalse(!paramKeys.contains(API_PARAM_PRODUCT_ID_LIST), "接口需要对产品ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_ID_LIST + "字段");
                Object fundIds = paramsJson.get(API_PARAM_PRODUCT_ID_LIST);
                isFalse(!(fundIds instanceof List), "参数" + API_PARAM_PRODUCT_ID_LIST + "应为list类型，请核对！");
                List<?> valList = (List<?>) fundIds;
                isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_ID_LIST + "为list类型，list中不允许空元素，请核对！");
                paramsMap.put(API_PARAM_PRODUCT_ID_LIST, StringUtils.join((List<?>) fundIds, ";"));
            }

            // 只配置了产品代码鉴权
            if (StringUtils.isNotEmpty(apiInterfaceVO.getFundCodesField()) && StringUtils.isEmpty(apiInterfaceVO.getFundIdsField())) {
                isFalse(paramsJsonNull, "接口需要对产品代码字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_CODE_LIST + "字段");
                isFalse(!paramKeys.contains(API_PARAM_PRODUCT_CODE_LIST), "接口需要对产品代码字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_CODE_LIST + "字段");
                Object fundCodes = paramsJson.get(API_PARAM_PRODUCT_CODE_LIST);
                isFalse(!(fundCodes instanceof List), "参数" + API_PARAM_PRODUCT_CODE_LIST + "应为list类型，请核对！");
                List<?> valList = (List<?>) fundCodes;
                isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_CODE_LIST + "为list类型，list中不允许空元素，请核对！");
                paramsMap.put(API_PARAM_PRODUCT_CODE_LIST, StringUtils.join((List<?>) fundCodes, ";"));
            }

            // 产品ID和产品代码都配置了鉴权，则二选一即可；若调用方两者都有传，则都进行鉴权
            if (StringUtils.isNotEmpty(apiInterfaceVO.getFundCodesField()) && StringUtils.isNotEmpty(apiInterfaceVO.getFundIdsField())) {
                isFalse(paramsJsonNull, "接口需要对产品代码或产品ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_CODE_LIST + "字段或" + API_PARAM_PRODUCT_ID_LIST + "字段");
                isFalse(!paramKeys.contains(API_PARAM_PRODUCT_CODE_LIST) && !paramKeys.contains(API_PARAM_PRODUCT_ID_LIST),
                        "接口需要对产品代码或产品ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_CODE_LIST + "字段或" + API_PARAM_PRODUCT_ID_LIST + "字段");
                Object fundIds = paramsJson.get(API_PARAM_PRODUCT_ID_LIST);
                Object fundCodes = paramsJson.get(API_PARAM_PRODUCT_CODE_LIST);
                isFalse(fundIds == null && fundCodes == null, "接口需要对产品代码或产品ID字段鉴权，请检查所传参数，确保上传" + API_PARAM_PRODUCT_CODE_LIST + "字段或" + API_PARAM_PRODUCT_ID_LIST + "字段");

                if (fundIds != null && fundCodes == null) {
                    isFalse(!(fundIds instanceof List), "参数" + API_PARAM_PRODUCT_ID_LIST + "应为list类型，请核对！");
                    List<?> valList = (List<?>) fundIds;
                    isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_ID_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_ID_LIST, StringUtils.join((List<?>) fundIds, ";"));
                }

                if (fundIds == null && fundCodes != null) {
                    isFalse(!(fundCodes instanceof List), "参数" + API_PARAM_PRODUCT_CODE_LIST + "应为list类型，请核对！");
                    List<?> valList = (List<?>) fundCodes;
                    isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_CODE_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_CODE_LIST, StringUtils.join((List<?>) fundCodes, ";"));
                }

                if (fundIds != null && fundCodes != null) {
                    isFalse(!(fundIds instanceof List), "参数" + API_PARAM_PRODUCT_ID_LIST + "应为list类型，请核对！");
                    List<?> valList = (List<?>) fundIds;
                    isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_ID_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_ID_LIST, StringUtils.join((List<?>) fundIds, ";"));

                    isFalse(!(fundCodes instanceof List), "参数" + API_PARAM_PRODUCT_CODE_LIST + "应为list类型，请核对！");
                    List<?> valList2 = (List<?>) fundCodes;
                    isFalse(valList2 != null && valList2.size() == 0, "参数" + API_PARAM_PRODUCT_CODE_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_CODE_LIST, StringUtils.join((List<?>) fundCodes, ";"));
                }
            }
        }

        // fieldAuth=false或者不传：鉴权主动权在接口调用端，若传鉴权参数则进行鉴权，若不传鉴权参数则不进行鉴权
        if (fieldAuth == null || fieldAuth.equals(false)) {
            if (!paramsJsonNull) {
                if (paramKeys.contains(API_PARAM_MANAGER_ID)) {
                    isFalse(StringUtils.isEmpty(apiInterfaceVO.getManagerField()), "接口未配置管理人ID鉴权字段，请检查！");
                    Object managerId = paramsJson.get(API_PARAM_MANAGER_ID);
                    boolean intCheck = false;
                    if (managerId instanceof Integer || managerId instanceof Long) {
                        intCheck = true;
                    } else if (managerId instanceof String) {
                        try {
                            intCheck = true;
                            Long.valueOf((String) managerId);
                        } catch (Exception e) {
                            intCheck = false;
                        }
                    }
                    isFalse(!intCheck, "参数" + API_PARAM_MANAGER_ID + "应为整型，请核对！");
                    paramsMap.put(API_PARAM_MANAGER_ID, managerId);
                }

                if (paramKeys.contains(API_PARAM_PRODUCT_ID_LIST)) {
                    isFalse(StringUtils.isEmpty(apiInterfaceVO.getFundIdsField()), "接口未配置产品ID鉴权字段，请检查！");
                    Object fundIds = paramsJson.get(API_PARAM_PRODUCT_ID_LIST);
                    isFalse(!(fundIds instanceof List), "参数" + API_PARAM_PRODUCT_ID_LIST + "应为list类型，请核对！");
                    List<?> valList = (List<?>) fundIds;
                    isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_ID_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_ID_LIST, StringUtils.join((List<?>) fundIds, ";"));
                }

                if (paramKeys.contains(API_PARAM_PRODUCT_CODE_LIST)) {
                    isFalse(StringUtils.isEmpty(apiInterfaceVO.getFundCodesField()), "接口未配置产品代码鉴权字段，请检查！");
                    Object fundCodes = paramsJson.get(API_PARAM_PRODUCT_CODE_LIST);
                    isFalse(!(fundCodes instanceof List), "参数" + API_PARAM_PRODUCT_CODE_LIST + "应为list类型，请核对！");
                    List<?> valList = (List<?>) fundCodes;
                    isFalse(valList != null && valList.size() == 0, "参数" + API_PARAM_PRODUCT_CODE_LIST + "为list类型，list中不允许空元素，请核对！");
                    paramsMap.put(API_PARAM_PRODUCT_CODE_LIST, StringUtils.join((List<?>) fundCodes, ";"));
                }
            }
        }
    }

    public String concatSql(ApiInterface apiInterface, Map<String, Object> paramsMap) {
        String sql = "";

        // SELECT
        String selectParam = apiInterface.getSelectParam();
        isFalse(StringUtils.isEmpty(selectParam), "接口select信息为空，请核对接口配置信息！");
        sql = sql + "SELECT " + selectParam;

        // FROM
        String fromParam = apiInterface.getFromParam();
        isFalse(StringUtils.isEmpty(fromParam), "接口from信息为空，请核对接口配置信息！");
        sql = sql + " FROM " + fromParam;

        // WHERE 20250717前台只显示一个where参数字段，即原来的where固定参数字段
        sql = sql + " WHERE ( 1 = 1 )";

        // WHERE固定
        String whereParamFixed = apiInterface.getWhereParamFixed();
        if (StringUtils.isNotEmpty(whereParamFixed)) {
            if (whereParamFixed.trim().startsWith("?AND") || whereParamFixed.trim().startsWith("?OR")) {
                sql = sql + " (" + whereParamFixed + ")";
            } else {
                sql = sql + " AND (" + whereParamFixed + ")";
            }
        }

        // WHERE可变（当前注释未启用）
    /*
    String whereParamChange = apiInterface.getWhereParamChange();
    if (StringUtils.isNotEmpty(whereParamChange)) {
        sql = sql + " AND (" + whereParamChange + ")";
    }
    */

        // 管理人ID、产品ID列表、产品代码列表
        if (paramsMap.containsKey(API_PARAM_MANAGER_ID)) {
            sql = sql + " AND " + apiInterface.getManagerField() + " = '" + paramsMap.get(API_PARAM_MANAGER_ID);
        }
        if (paramsMap.containsKey(API_PARAM_PRODUCT_ID_LIST)) {
            sql = sql + " AND @IN{" + API_PARAM_PRODUCT_ID_LIST + ", " + apiInterface.getFundIdsField() + "}";
        }
        if (paramsMap.containsKey(API_PARAM_PRODUCT_CODE_LIST)) {
            sql = sql + " AND @IN{" + API_PARAM_PRODUCT_CODE_LIST + ", " + apiInterface.getFundCodesField() + "}";
        }

        // GROUP
        String groupParam = apiInterface.getGroupParam();
        if (StringUtils.isNotEmpty(groupParam)) {
            sql = sql + " GROUP BY " + groupParam;
        }

        // ORDER
        String orderParam = apiInterface.getOrderParam();
        if (StringUtils.isNotEmpty(orderParam)) {
            sql = sql + " ORDER BY " + orderParam;
        }

        return sql;
    }
}
