package com.citics.glxtapi.web.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citics.glxtapi.common.factory.PageFactory;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.common.utils.page.PageUtils;
import com.citics.glxtapi.plugin.db.domain.ConnectionBaseInfo;
import com.citics.glxtapi.plugin.db.domain.DbConnectionInfo;
import com.citics.glxtapi.web.entity.Connection;
import com.citics.glxtapi.web.mapper.ConnectionMapper;
import com.citics.glxtapi.web.service.ApiService;
import com.citics.glxtapi.web.service.ConnectionService;
import com.citics.glxtapi.web.service.TenantService;
import com.citics.glxtapi.web.service.factory.ConnectionAdapteServiceFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.Assert.isFalse;

/**
 * 连接 服务实现类
 */
@Service
public class ConnectionServiceImpl extends ServiceImpl<ConnectionMapper, Connection> implements ConnectionService {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ApiService apiService;

    @Autowired
    private ConnectionAdapteServiceFactory connectionAdapteServiceFactory;

    @Override
    public ConnectionBaseInfo test(DbConnectionInfo dto) {
        isFalse(StringUtils.isEmpty(dto.getName()), "名称不能为空，请核对！");
        isFalse(StringUtils.isEmpty(dto.getKey()), "编码不能为空，请核对！");
        isFalse(StringUtils.isEmpty(dto.getType()), "未指定数据源类型，请核对！");
        return connectionAdapteServiceFactory.test(dto.getType(), JSON.toJSONString(dto));
    }

    @Override
    public ConnectionBaseInfo save(DbConnectionInfo dto) {
        ConnectionBaseInfo connectionBaseInfo = this.test(dto);
        isFalse(null == connectionBaseInfo, "测试连接时失败，请核对！");
        Connection connection = new Connection();
        this.connectionAdapteServiceFactory.encode(connection, connectionBaseInfo);
//        if (dto.getMaxRows() != null) {
//            isFalse(dto.getMaxRows() < 1 || dto.getMaxRows() > 10000, "数据源最大行请配置1-10000区间的整数");
//        }
        if (connection.isNew()) {
            isFalse(this.isHaveConnection(connection.getCode()),
                    "存在【" + connection.getCode() + "】连接，请核对！");
            this.connectionAdapteServiceFactory.save(connectionBaseInfo);
            // 添加租户标记
            String tenant = this.tenantService.getTenant();
            if (StrUtil.isNotEmpty(tenant)) {
                connection.setTenant(tenant);
            }
            // 保存连接
            this.save(connection);
        } else {
            Connection oldConnection = this.getById(connection.getId());
            isFalse(null == oldConnection, "未找到对应连接，无法更新，请核对！");
            String oldKey = oldConnection.getCode();
            this.connectionAdapteServiceFactory.update(connectionBaseInfo, oldKey);
            if (StrUtil.isEmpty(oldConnection.getTenant())) {
                String tenant = this.tenantService.getTenant();
                if (StrUtil.isNotEmpty(tenant)) {
                    connection.setTenant(tenant);
                }
            }
            // 更新连接
            this.updateById(connection);
        }
        return this.connectionAdapteServiceFactory.decode(connection);
    }

    @Override
    public List<ConnectionBaseInfo> list(String type) {
        QueryWrapper<Connection> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(Connection::getType, type);
        }
        String tenant = this.tenantService.getTenant();
        if (StrUtil.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(Connection::getTenant, tenant);
        }
        List<Connection> connectionList = this.list(queryWrapper);
        List<ConnectionBaseInfo> connectionBaseInfoList = new ArrayList<>();
        connectionList.forEach(connection -> connectionBaseInfoList.add(this.connectionAdapteServiceFactory.decode(connection)));
        return connectionBaseInfoList;
    }

    @Override
    public PageResult page(DbConnectionInfo dto) {
        QueryWrapper<Connection> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotEmpty(dto.getType())) {
            queryWrapper.lambda().eq(Connection::getType, dto.getType());
        }
        queryWrapper.orderByAsc("id");
        String tenant = this.tenantService.getTenant();
        if (StrUtil.isNotEmpty(tenant)) {
            queryWrapper.lambda().eq(Connection::getTenant, tenant);
        }
        PageResult pr = PageUtils.getPageResult(this.page(PageFactory.jsonPage(dto.getPageNum(), dto.getPageSize()), queryWrapper));
        List<Connection> connectionList = (List<Connection>) pr.getContent();
        List<ConnectionBaseInfo> connectionBaseInfoList = new ArrayList<>();
        connectionList.forEach(connection -> connectionBaseInfoList.add(this.connectionAdapteServiceFactory.decode(connection)));
        pr.setContent(connectionBaseInfoList);
        return pr;
    }

    @Override
    public List<ConnectionBaseInfo> listAll(String type) {
        QueryWrapper<Connection> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(Connection::getType, type);
        }
        List<Connection> connectionList = this.list(queryWrapper);
        List<ConnectionBaseInfo> connectionBaseInfoList = new ArrayList<>();
        connectionList.forEach(connection -> connectionBaseInfoList.add(this.connectionAdapteServiceFactory.decode(connection)));
        return connectionBaseInfoList;
    }

    @Override
    public Boolean initialize() {
        this.connectionAdapteServiceFactory.initialize("db", this.listAll("db"));
        return true;
    }

    @Override
    public Boolean sync() {
        List<ConnectionBaseInfo> connectionBaseInfoList = this.list("db");
        if (null != connectionBaseInfoList && connectionBaseInfoList.size() != 0) {
            Map<String, List<ConnectionBaseInfo>> connectionMap = connectionBaseInfoList.stream().collect(Collectors.groupingBy(ConnectionBaseInfo::getType));
            connectionMap.forEach((type, connectionList) -> this.connectionAdapteServiceFactory.sync(type, connectionList));
        }
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        Connection connection = this.getById(id);
        isFalse(null == connection, "未找到对应连接，无法删除，请核对！");
        isFalse(this.apiService.isHaveConnection(connection.getId()), "该数据源存在接口配置信息，不可删除！");
        this.connectionAdapteServiceFactory.delete(connection.getType(), connection.getCode());
        return super.removeById(id);
    }

    /**
     * 判断是否存在相同的key
     * @param key key
     * @return
     */
    private Boolean isHaveConnection(String key) {
        QueryWrapper<Connection> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Connection::getCode, key);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Boolean isHaveConnectionPermission(String tenant, Long connectionId) {
        Connection conn = this.getById(connectionId);
        return StringUtils.isNotEmpty(conn.getTenant()) && StringUtils.isNotEmpty(tenant) && conn.getTenant().equals(tenant);
    }

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    @Override
    public Boolean isHaveTenant(String tenant) {
        QueryWrapper<Connection> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Connection::getTenant, tenant);
        return this.count(queryWrapper) > 0;
    }
}
