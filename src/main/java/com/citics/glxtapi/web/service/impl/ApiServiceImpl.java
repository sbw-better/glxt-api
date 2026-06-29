package com.citics.glxtapi.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.mapper.ApiMapper;
import com.citics.glxtapi.web.service.ApiParamService;
import com.citics.glxtapi.web.service.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;

@Slf4j
@Service
public class ApiServiceImpl extends ServiceImpl<ApiMapper, ApiInterface> implements ApiService {

    @Autowired
    ApiParamService apiParamService;

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
            // queryWrapper.lambda().eq(ApiInterface::getCode, code);
            queryWrapper.lambda().apply("UPPER(code) LIKE UPPER({0})", "%" + code + "%");
        }
        queryWrapper.lambda().like(StringUtils.isNotEmpty(name), ApiInterface::getName, name);
        return this.getOne(queryWrapper);
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

    /**
     * 解码
     * @param apiInterface
     * @param vo 接口配置信息
     */
    private void decode(ApiInterface apiInterface, ApiInterfaceVO vo) {
        if (null == vo) {
            return;
        }
        // 编码公共参数
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
}
