package com.citics.glxtapi.web.service.impl;

import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.common.factory.PageFactory;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.utils.page.PageUtils;
import com.citics.glxtapi.common.utils.poi.MultiExcelUtil;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.InterfaceDemoVo;
import com.citics.glxtapi.web.mapper.ApiMapper;
import com.citics.glxtapi.web.service.*;
import com.citics.glxtapi.web.support.TenantContextHolder;
import com.citics.glxtapi.web.support.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;
import static com.citics.glxtapi.web.constants.Constants.*;

@Slf4j
@Service
public class ApiServiceImpl extends ServiceImpl<ApiMapper, ApiInterface> implements ApiService {

    @Value("${common.demo_path}")
    private String demoPath;

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    ApiParamService apiParamService;
    @Autowired
    TenantInterfaceService tenantInterfaceService;

    @Override
    public long count(String tenant) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        }
        return this.count(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiInterfaceVO save(ApiInterfaceDTO dto) {
        isFalse(null == dto.getApiParamList(), "参数字段可为空：[]，但必传，请核对！");
        List<ApiParam> apiParamList = new ArrayList<>(dto.getApiParamList());
        String tenant = this.tenantService.getTenant();
        tenant = StringUtils.isEmpty(tenant) ? dto.getTenant() : tenant;

        // 数据校验
        isFalse(StringUtils.isEmpty(dto.getName()), "名称不能为空，请核对！");
        isFalse(StringUtils.isEmpty(dto.getCode()), "编码不能为空，请核对！");
        isFalse(StringUtils.isEmpty(dto.getSelectParam()), "SELECT参数不能为空，请核对！");
        isFalse(dto.getFieldBackMode() == null, "字段返回模式不能为空，请核对！");
        isFalse(StringUtils.isEmpty(dto.getFromParam()), "FROM参数不能为空，请核对！");
        isFalse(dto.getPage() == null, "是否分页未指定，请核对！");
        isFalse(dto.getConnectionId() == null, "数据源未指定，请核对！");
        isFalse(!this.connectionService.isHaveConnectionPermission(tenant, dto.getConnectionId()), "数据源未授权，请核对！");

        for (ApiParam apiParam : apiParamList) {
            String code = apiParam.getCode();
            isFalse(StringUtils.isEmpty(apiParam.getName()), "入参名称不能为空，请核对！");
            isFalse(StringUtils.isEmpty(code), "入参编码不能为空，请核对！");
            isFalse(apiParam.getType() == null, "入参类型不能为空，请核对！");
            isFalse(apiParam.getRequired() == null, "入参是否必填不能为空，请核对！");
            isFalse(apiParam.getValidateType() == null, "入参校验类型不能为空，请核对！");

            if (apiParam.getValidateType() != FILED_CHECK_TYPE_NO) {
                isFalse(StringUtils.isEmpty(apiParam.getExpression()), "入参校验表达式不能为空，请核对！");
                isFalse(StringUtils.isEmpty(apiParam.getError()), "入参校验错误提示不能为空，请核对！");
            }

            if (code.equals(API_PARAM_MANAGER_ID) || code.equals(API_PARAM_PRODUCT_ID_LIST) || code.equals(API_PARAM_PRODUCT_CODE_LIST)) {
                isFalse(apiParam.getRequired().equals(WHETHER_NO), "入参managerField、fundIdsField、fundCodesField，需配置为非必填，请核对！");
                if (code.equals(API_PARAM_MANAGER_ID)) {
                    isFalse(!apiParam.getType().equals(FILED_TYPE_INT), "入参managerField的类型应为整型，请核对！");
                } else {
                    isFalse(!apiParam.getType().equals(FILED_TYPE_STRING), "入参fundIdsField、fundCodesField的类型应为字符串类型，请核对！");
                }
            }
        }

        ApiInterface apiInterface = new ApiInterface();
        this.encode(apiInterface, dto);
        // 添加租户标记
        if (StringUtils.isNotEmpty(tenant)) {
            apiInterface.setTenant(tenant);
        }

        if (apiInterface.isNew()) {
            isFalse(this.isHaveApi(tenant, apiInterface.getCode()), "已经存在【" + dto.getCode() + "】接口，请核对！");
            apiInterface.setCreateBy(UserContextHolder.getUserId());
            apiInterface.setCreateTime(new Date());
            this.save(apiInterface);
        } else {
            ApiInterface oldApiInterface = this.getById(apiInterface.getId());
            isFalse(null == oldApiInterface, "未找到对应接口配置，无法更新，请核对！");
            apiInterface.setUpdateBy(UserContextHolder.getUserId());
            apiInterface.setUpdateTime(new Date());
            this.updateById(apiInterface);
        }

        // 新增子表
        this.apiParamService.save(apiInterface.getId(), apiParamList);

        ApiInterfaceVO vo = new ApiInterfaceVO();
        this.decode(apiInterface, vo);
        vo.setApiParamList(apiParamList);
        return vo;
    }

    @Override
    public Boolean isPage(ApiInterfaceDTO dto) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(ApiInterface::getCode, dto.getCode())
                .eq(ApiInterface::getTenant, dto.getTenant());
        Integer isPage = this.getOne(queryWrapper).getPage();
        return isPage > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Serializable id) {
        ApiInterface api = this.getById(id);
        isFalse(null == api, "未找到对应接口配置，无法删除，请核对！");
        boolean res = this.removeById(id);
        boolean subRes = this.apiParamService.delete(id);
        boolean authRes = this.tenantInterfaceService.deleteAuthIdByApiId(id);
        return res && subRes && authRes;
    }

    @Override
    public ApiInterfaceVO getByApi(String tenant, String code) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        queryWrapper.lambda().eq(ApiInterface::getCode, code);
        long count = this.count(queryWrapper);
        isFalse(count < 1, "不存在编码为" + code + "的接口，请核对！");
        ApiInterfaceVO vo = new ApiInterfaceVO();
        if (StringUtils.isEmpty(code)) {
            return vo;
        }
        ApiInterface apiInterface = this.getOne(queryWrapper);
        if (null == apiInterface) {
            return vo;
        }
        List<ApiParam> apiParamList = apiParamService.list(apiInterface.getId());
        decode(apiInterface, vo);
        vo.setApiParamList(apiParamList);
        return vo;
    }

    @Override
    public ApiInterface getByCodeName(String code, String name) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(code)) {
            // 不区分大小写模糊匹配
            queryWrapper.lambda().apply("UPPER(code) LIKE UPPER({0})", "%" + code + "%");
        }
        queryWrapper.lambda().like(StringUtils.isNotEmpty(name), ApiInterface::getName, name);
        return this.getOne(queryWrapper);
    }

    @Override
    public ApiInterfaceVO get(Long id, String code) {
        isFalse(id == null && StringUtils.isEmpty(code), "ID和编码不能均为空，请核对！");
        ApiInterfaceVO vo = new ApiInterfaceVO();
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();

        // 设置租户信息
        String tenant = this.tenantService.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        }

        if (id != null) {
            queryWrapper.lambda().eq(ApiInterface::getId, id);
        }
        if (StringUtils.isNotEmpty(code)) {
            queryWrapper.lambda().eq(ApiInterface::getCode, code);
        }

        ApiInterface apiInterface = this.getOne(queryWrapper);
        if (null == apiInterface) {
            return vo;
        }

        List<ApiParam> apiParamList = apiParamService.list(apiInterface.getId());
        decode(apiInterface, vo);
        vo.setApiParamList(apiParamList);
        return vo;
    }

    @Override
    public List<ApiInterface> list(ApiInterfaceDTO dto) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        // 设置租户信息
        String tenant = this.tenantService.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        }

        // 接口名称模糊匹配
        queryWrapper.lambda().like(StringUtils.isNotEmpty(dto.getName()), ApiInterface::getName, dto.getName());

        // 接口代码匹配
        if (StringUtils.isNotEmpty(dto.getCode())) {
            queryWrapper.lambda().eq(ApiInterface::getCode, dto.getCode());
        }

        // 接口类型匹配
        if (null != dto.getType()) {
            queryWrapper.lambda().eq(ApiInterface::getType, dto.getType());
        }

        // 接口是否分页匹配
        if (null != dto.getPage()) {
            queryWrapper.lambda().eq(ApiInterface::getPage, dto.getPage());
        }

        // 接口数据源ID匹配
        if (null != dto.getConnectionId()) {
            queryWrapper.lambda().eq(ApiInterface::getConnectionId, dto.getConnectionId());
        }

        // 排序
        queryWrapper.orderByAsc("order_no");
        queryWrapper.orderByAsc("id");
        return this.list(queryWrapper);
    }

    @Override
    public PageResult page(ApiInterfaceDTO dto) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        // 设置租户信息
        String tenant = this.tenantService.getTenant();
        if (StringUtils.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        }

        // 接口名称模糊匹配
        queryWrapper.lambda().like(StringUtils.isNotEmpty(dto.getName()), ApiInterface::getName, dto.getName());

        // 接口代码匹配（不区分大小写模糊查询）
        if (StringUtils.isNotEmpty(dto.getCode())) {
            queryWrapper.lambda().apply("UPPER(code) LIKE UPPER({0})", "%" + dto.getCode() + "%");
        }

        // 接口类型匹配
        if (null != dto.getType()) {
            queryWrapper.lambda().eq(ApiInterface::getType, dto.getType());
        }

        // 接口是否分页匹配
        if (null != dto.getPage()) {
            queryWrapper.lambda().eq(ApiInterface::getPage, dto.getPage());
        }

        // 接口数据源ID匹配
        if (null != dto.getConnectionId()) {
            queryWrapper.lambda().eq(ApiInterface::getConnectionId, dto.getConnectionId());
        }

        // 接口描述模糊匹配
        if (StringUtils.isNotEmpty(dto.getDescription())) {
            queryWrapper.lambda().like(ApiInterface::getDescription, dto.getDescription());
        }

        // 接口SQL模糊匹配（去除多余空格，忽略大小写）
        if (StringUtils.isNotEmpty(dto.getPreviewSql())) {
            String sqlParam = dto.getPreviewSql().replace(" ", "");
            queryWrapper.lambda().apply("REPLACE(UPPER(PREVIEW_SQL),' ','') LIKE UPPER({0})", "%" + sqlParam + "%");
        }

        // 排序
        queryWrapper.orderByAsc("order_no");
        queryWrapper.orderByAsc("id");

        // 分页查询并组装分页结果
        return PageUtils.getPageResult(this.page(PageFactory.jsonPage(dto.getPageNum(), dto.getPageSize()), queryWrapper));
    }

    @Override
    public String preview(ApiInterfaceDTO dto) {
        String sql = "";
        // SELECT
        String selectParam = dto.getSelectParam();
        isFalse(StringUtils.isEmpty(selectParam), "接口select信息为空，暂不可预览，请先配置！");
        sql = sql + "SELECT " + selectParam;

        // FROM
        String fromParam = dto.getFromParam();
        isFalse(StringUtils.isEmpty(fromParam), "接口from信息为空，暂不可预览，请先配置！");
        sql = sql + " FROM " + fromParam;

        // WHERE 管理人ID、产品ID列表、产品代码列表
        sql = sql + " WHERE ( 1 = 1 )";
        if (StringUtils.isNotEmpty(dto.getManagerField())) {
            sql = sql + " AND " + dto.getManagerField() + " = " + "#{managerField}";
        }
        if (StringUtils.isNotEmpty(dto.getFundIdsField())) {
            sql = sql + " AND @IN{fundIdsField, " + dto.getFundIdsField() + "}";
        }
        if (StringUtils.isNotEmpty(dto.getFundCodesField())) {
            sql = sql + " AND @IN{fundCodesField, " + dto.getFundCodesField() + "}";
        }

        // WHERE固定条件
        String whereParamFixed = dto.getWhereParamFixed();
        if (StringUtils.isNotEmpty(whereParamFixed)) {
            sql = sql + " AND (" + whereParamFixed + ")";
        }

        // WHERE可变条件
        String whereParamChange = dto.getWhereParamChange();
        if (StringUtils.isNotEmpty(whereParamChange)) {
            sql = sql + " AND (" + whereParamChange + ")";
        }

        // GROUP BY
        String groupParam = dto.getGroupParam();
        if (StringUtils.isNotEmpty(groupParam)) {
            sql = sql + " GROUP BY " + groupParam;
        }

        // ORDER BY
        String orderParam = dto.getOrderParam();
        if (StringUtils.isNotEmpty(orderParam)) {
            sql = sql + " ORDER BY " + orderParam;
        }

        // 正则替换处理占位符
        String regex = "";
        Pattern pt = null;
        Matcher mc = null;

        // 替换 #{变量}
        regex = "(#\\{(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), "【变量" + mc.group(2) + "】");
            mc = pt.matcher(sql);
        }

        // 替换 @IN{key,col}
        regex = "(@IN\\{(.*?),(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), mc.group(3) + " IN (【变量" + mc.group(2) + "】)");
            mc = pt.matcher(sql);
        }

        // 替换 @LIKE{key,col}
        regex = "(@LIKE\\{(.*?),(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), mc.group(3) + " LIKE (【变量 %" + mc.group(2) + "%】)");
            mc = pt.matcher(sql);
        }

        // 替换 @LIKER{key,col}
        regex = "(@LIKER\\{(.*?),(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), mc.group(3) + " LIKE (【变量 " + mc.group(2) + "%】)");
            mc = pt.matcher(sql);
        }

        // 替换 @LIKEL{key,col}
        regex = "(@LIKEL\\{(.*?),(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), mc.group(3) + " LIKE (【变量 %" + mc.group(2) + "】)");
            mc = pt.matcher(sql);
        }

        // 替换 ?{变量,提示}
        regex = "(\\?\\{(.*?),(.*?)})";
        pt = Pattern.compile(regex);
        mc = pt.matcher(sql);
        while (mc.find()) {
            sql = sql.replace(mc.group(1), mc.group(3) + "【变量不传时此条件清空】");
            mc = pt.matcher(sql);
        }

        return sql;
    }

    @Override
    public InterfaceDemoVo demo(ApiInterfaceDTO dto) {
        // 获取接口
        InterfaceDemoVo demo = new InterfaceDemoVo();
        String apiCode = dto.getCode();
        isFalse(StringUtils.isEmpty(apiCode), "接口代码必传，请检查！");
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiInterface::getCode, apiCode);
        ApiInterface api = this.getOne(queryWrapper);
        isFalse(null == api, "根据接口代码所查询接口为空，请检查！");

        // 获取接口参数
        List<ApiParam> apiParamList = this.get(api.getId(), api.getCode()).getApiParamList();

        // body生成
        JSONObject jsonObject = new JSONObject(true);
        jsonObject.put("token", "替换为接口所在分组的token值");
        jsonObject.put("systemCode", "替换为接口调用方的系统代码，如\"glxt\"");
        jsonObject.put("tenant", TenantContextHolder.getTenant());
        jsonObject.put("apiCode", api.getCode());
        jsonObject.put("fieldAuth", "鉴权开关（一般可不传该字段）");
        jsonObject.put("pageNeed", "分页开关（一般可不传该字段）");
        if (WHETHER_YES.equals(api.getPage())) {
            jsonObject.put("pageNum", 1);
            jsonObject.put("pageSize", 2);
        }

        if (apiParamList != null && apiParamList.size() != 0) {
            JSONObject jsonObjectSub = new JSONObject(true);
            for (ApiParam p : apiParamList) {
                Integer type = p.getType();
                String key = WHETHER_YES.equals(p.getRequired()) ? p.getCode() : p.getCode() + "(非必传，不传取默认值，传则删掉括号内容)";
                if (FILED_TYPE_STRING == type) {
                    jsonObjectSub.put(key, "XXX");
                } else if (FILED_TYPE_INT == type) {
                    jsonObjectSub.put(key, 0);
                } else if (FILED_TYPE_FLOAT == type) {
                    jsonObjectSub.put(key, 1.0);
                } else if (FILED_TYPE_DATE == type) {
                    jsonObjectSub.put(key, "yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
                } else {
                    jsonObjectSub.put(key, "XXX");
                }
            }
            jsonObject.put("params", jsonObjectSub);
        }

        if (StringUtils.isNotEmpty(api.getManagerField())) {
            JSONObject params;
            if (jsonObject.containsKey("params")) {
                params = (JSONObject) jsonObject.get("params");
            } else {
                params = new JSONObject(true);
            }
            params.put("managerField(非必传，传则删掉括号内容)", 525);
            jsonObject.put("params", params);
        }

        if (StringUtils.isNotEmpty(api.getFundIdsField())) {
            JSONObject params;
            if (jsonObject.containsKey("params")) {
                params = (JSONObject) jsonObject.get("params");
            } else {
                params = new JSONObject(true);
            }
            params.put("fundIdsField(非必传，传则删掉括号内容)", "[1,2,3,4,5]");
            jsonObject.put("params", params);
        }

        if (StringUtils.isNotEmpty(api.getFundCodesField())) {
            JSONObject params;
            if (jsonObject.containsKey("params")) {
                params = (JSONObject) jsonObject.get("params");
            } else {
                params = new JSONObject(true);
            }
            params.put("fundCodesField(非必传，传则删掉括号内容)", "[CPDM1,CPDM2,CPDM3]");
            jsonObject.put("params", params);
        }

        demo.setBody(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat));

        // url生成
        demo.setUrl(this.demoPath);

        return demo;
    }

    /**
     * 导出接口配置信息及其参数信息
     * @param dto
     * @return
     */
    @Override
    public String export(ApiInterfaceDTO dto) {
        String fileName = null;
        List<Long> ids = dto.getIds();
        isFalse(null == ids, "导出操作记录ID串为空，无法导出，请检查！");

        // 处理接口配置
        List<ApiInterface> apiInterfaceList = new ArrayList<>();
        QueryWrapper<ApiInterface> wrapper = new QueryWrapper<>();
        for (int i = 0; i < ids.size(); i += 200) {
            wrapper.clear();
            List<Long> idsSub = new ArrayList<>();
            for (int j = i; j < Math.min(i + 200, ids.size()); j++) {
                idsSub.add(ids.get(j));
            }
            wrapper.lambda().in(ApiInterface::getId, idsSub);
            apiInterfaceList.addAll(this.list(wrapper));
        }

        // 处理接口参数
        List<ApiParam> apiParamList = new ArrayList<>();
        QueryWrapper<ApiParam> wrapperParam = new QueryWrapper<>();
        for (int i = 0; i < ids.size(); i += 20) {
            wrapperParam.clear();
            List<Long> idsSub = new ArrayList<>();
            for (int j = i; j < Math.min(i + 20, ids.size()); j++) {
                idsSub.add(ids.get(j));
            }
            wrapperParam.lambda().in(ApiParam::getApiId, idsSub);
            apiParamList.addAll(this.apiParamService.list(wrapperParam));
        }

        MultiExcelUtil excelUtil = MultiExcelUtil.init();
        // SHEET1
        excelUtil.createSheet(apiInterfaceList, SHEET_INTERFACE, ApiInterface.class);
        // SHEET2
        excelUtil.createSheet(apiParamList, SHEET_INTERFACE_PARAM, ApiParam.class);

        fileName = excelUtil.exportName(EXCEL_INTERFACE);
        return fileName;
    }

    /**
     * 导入接口配置信息及其参数信息
     * @param file
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean upload(MultipartFile file) throws Exception {
        try {
            isFalse(null == file, "上传EXCEL文件为空，无法导入，请检查！");
            String fileName = file.getOriginalFilename();
            isFalse(StringUtils.isEmpty(fileName), "上传的文件名为空，无法导入，请检查！");
            String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            isFalse(!"xls".equals(fileSuffix) && !"xlsx".equals(fileSuffix), "文件格式非EXCEL，无法导入，请检查！");

            // 解析文件，获取实例对象list
            MultiExcelUtil excelUtil = MultiExcelUtil.init();
            List<ApiInterface> apiInterfaceList = excelUtil.analyzeSheet(file, SHEET_INTERFACE, ApiInterface.class);
            List<ApiParam> apiParamList = excelUtil.analyzeSheet(file, SHEET_INTERFACE_PARAM, ApiParam.class);

            if (apiInterfaceList != null && apiInterfaceList.size() != 0) {
                QueryWrapper<ApiInterface> wrapperApi = new QueryWrapper<>();
                UpdateWrapper<ApiInterface> updateWrapper = new UpdateWrapper<>();

                for (ApiInterface api : apiInterfaceList) {
                    wrapperApi.clear();
                    wrapperApi.lambda().eq(ApiInterface::getCode, api.getCode());
                    isFalse(this.count(wrapperApi) > 0, "文件中的接口" + api.getCode() + "已存在，导入失败，请检查！");

                    List<ApiParam> apiSubParams = apiParamList.stream()
                            .filter(x -> x.getApiId().equals(api.getId()))
                            .collect(Collectors.toList());

                    // 插入接口和参数
                    // 接口ID置空
                    api.setId(null);
                    // 先改成默认数据源
                    api.setConnectionId(null);
                    ApiInterfaceDTO dto = new ApiInterfaceDTO();
                    BeanUtils.copyProperties(api, dto);
                    dto.setApiParamList(new ArrayList<>());
                    Long apiId = this.save(dto).getId();

                    // 插入成功后再将数据源ID置空
                    updateWrapper.clear();
                    updateWrapper.lambda()
                            .set(ApiInterface::getConnectionId, null)
                            .eq(ApiInterface::getId, apiId);
                    this.update(updateWrapper);

                    if (apiSubParams != null && apiSubParams.size() != 0) {
                        QueryWrapper<ApiParam> wrapperParam = new QueryWrapper<>();
                        for (ApiParam param : apiSubParams) {
                            param.setApiId(apiId);
                            // 参数ID置空
                            param.setId(null);
                            wrapperParam.clear();
                            wrapperParam.lambda()
                                    .eq(ApiParam::getApiId, param.getApiId())
                                    .eq(ApiParam::getCode, param.getCode());
                            isFalse(this.apiParamService.count(wrapperParam) > 0,
                                    "文件中的参数" + param.getCode() + "在对应的接口中已存在，导入失败，请检查！");
                            this.apiParamService.save(param);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("文件导入时失败，请检查处理：" + e.getMessage());
        }
        return true;
    }

    private Boolean isHaveApi(String tenant, String code) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        queryWrapper.lambda().eq(ApiInterface::getCode, code);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 编码
     * @param apiInterface
     * @param dto 接口配置信息
     */
    private void encode(ApiInterface apiInterface, ApiInterfaceDTO dto) {
        if (null == apiInterface) {
            return;
        }
        // 编码公共参数
        apiInterface.setId(dto.getId());
        apiInterface.setName(dto.getName());
        apiInterface.setCode(dto.getCode());
        apiInterface.setType(dto.getType());
        apiInterface.setDescription(dto.getDescription());
        apiInterface.setOrderNo(dto.getOrderNo());
        apiInterface.setSelectParam(dto.getSelectParam());
        apiInterface.setFieldBackMode(dto.getFieldBackMode());
        apiInterface.setFromParam(dto.getFromParam());
        apiInterface.setWhereParamFixed(dto.getWhereParamFixed());
        apiInterface.setWhereParamChange(dto.getWhereParamChange());
        apiInterface.setGroupParam(dto.getGroupParam());
        apiInterface.setOrderParam(dto.getOrderParam());
        apiInterface.setPage(dto.getPage());
        apiInterface.setManagerField(dto.getManagerField());
        apiInterface.setFundIdsField(dto.getFundIdsField());
        apiInterface.setFundCodesField(dto.getFundCodesField());
        apiInterface.setConnectionId(dto.getConnectionId());
        apiInterface.setPreviewSql(this.preview(dto));
    }

    /**
     * 解码
     * @param apiInterface
     * @param vo 接口配置信息
     */
    private void decode(ApiInterface apiInterface, ApiInterfaceVO vo) {
        if (null == vo) {
            return;
        }
        // 赋值公共参数
        vo.setTenant(apiInterface.getTenant());
        vo.setId(apiInterface.getId());
        vo.setName(apiInterface.getName());
        vo.setCode(apiInterface.getCode());
        vo.setType(apiInterface.getType());
        vo.setDescription(apiInterface.getDescription());
        vo.setOrderNo(apiInterface.getOrderNo());
        vo.setSelectParam(apiInterface.getSelectParam());
        vo.setFieldBackMode(apiInterface.getFieldBackMode());
        vo.setFromParam(apiInterface.getFromParam());
        vo.setWhereParamFixed(apiInterface.getWhereParamFixed());
        vo.setWhereParamChange(apiInterface.getWhereParamChange());
        vo.setGroupParam(apiInterface.getGroupParam());
        vo.setOrderParam(apiInterface.getOrderParam());
        vo.setPage(apiInterface.getPage());
        vo.setManagerField(apiInterface.getManagerField());
        vo.setFundIdsField(apiInterface.getFundIdsField());
        vo.setFundCodesField(apiInterface.getFundCodesField());
        vo.setConnectionId(apiInterface.getConnectionId());
    }

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    @Override
    public Boolean isHaveTenant(String tenant) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiInterface::getTenant, tenant);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 判断是否存在该connection数据
     * @param connectionId
     * @return
     */
    @Override
    public Boolean isHaveConnection(Long connectionId) {
        QueryWrapper<ApiInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ApiInterface::getConnectionId, connectionId);
        return this.count(queryWrapper) > 0;
    }
}
