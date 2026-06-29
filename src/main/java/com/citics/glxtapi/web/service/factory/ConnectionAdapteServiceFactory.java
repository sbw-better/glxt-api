package com.citics.glxtapi.web.service.factory;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.citics.glxtapi.plugin.db.domain.ConnectionBaseInfo;
import com.citics.glxtapi.plugin.db.domain.ConnectionExtendConfigInfo;
import com.citics.glxtapi.plugin.db.service.Impl.DbConnectionAdapteServiceImpl;
import com.citics.glxtapi.web.entity.Connection;
import com.citics.glxtapi.plugin.db.service.ConnectionAdapteService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态扫描连接转换接口实现
 */
@Component
public class ConnectionAdapteServiceFactory {

    public Boolean initialize(String type, List<ConnectionBaseInfo> connectionBaseInfoList) {
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return false;
        }
        return connectionAdapteService.initialize(connectionBaseInfoList);
    }

    public Boolean destroy(String type) {
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return false;
        }
        return connectionAdapteService.destroy();
    }

    public Boolean save(ConnectionBaseInfo connectionBaseInfo) {
        String type = connectionBaseInfo.getType();
        if (StrUtil.isEmpty(type)) {
            return false;
        }
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return false;
        }
        return connectionAdapteService.save(connectionBaseInfo);
    }

    public Boolean update(ConnectionBaseInfo connectionBaseInfo, String oldKey) {
        String type = connectionBaseInfo.getType();
        if (StrUtil.isEmpty(type)) {
            return false;
        }
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return false;
        }
        return connectionAdapteService.update(connectionBaseInfo, oldKey);
    }

    public Boolean delete(String type, String key) {
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return false;
        }
        return connectionAdapteService.delete(key);
    }

    /**
     * 编码
     * @param connection
     * @param connectionBaseInfo 连接信息
     */
    public void encode(Connection connection, ConnectionBaseInfo connectionBaseInfo) {
        if (null == connection) {
            return;
        }
        // 编码公共参数
        connection.setId(connectionBaseInfo.getId());
        connection.setName(connectionBaseInfo.getName());
        connection.setCode(connectionBaseInfo.getKey());
        connection.setType(connectionBaseInfo.getType());
        connection.setTimeout(connectionBaseInfo.getTimeout());

        List<ConnectionExtendConfigInfo> extendConfigList = connectionBaseInfo.getExtendConfigList();
        if (null != extendConfigList && extendConfigList.size() != 0) {
            JSONArray jsonArray = new JSONArray();
            extendConfigList.forEach(connectionExtendConfigInfo -> jsonArray.put(JSONUtil.parseObj(connectionExtendConfigInfo)));
            connection.setExtendConfig(JSONUtil.toJsonStr(jsonArray));
        }

        ConnectionAdapteService connectionAdapteService = this.getService(connectionBaseInfo.getType());
        if (null == connectionAdapteService) {
            return;
        }
        connection.setConfig(connectionAdapteService.encode(connectionBaseInfo));
    }

    /**
     * 解码
     * @param connection
     * @return
     */
    public ConnectionBaseInfo decode(Connection connection) {
        if (null == connection) {
            return null;
        }
        // 解码公共参数
        ConnectionBaseInfo connectionBaseInfo = new ConnectionBaseInfo();
        connectionBaseInfo.setTenant(connection.getTenant());
        connectionBaseInfo.setId(connection.getId());
        connectionBaseInfo.setName(connection.getName());
        connectionBaseInfo.setKey(connection.getCode());
        connectionBaseInfo.setType(connection.getType());
        connectionBaseInfo.setTimeout(connection.getTimeout());

        String extendConfig = connection.getExtendConfig();
        if (StrUtil.isNotEmpty(extendConfig)) {
            List<ConnectionExtendConfigInfo> extendConfigList = new ArrayList<>();
            JSONArray jsonArray = JSONUtil.parseArray(extendConfig);
            jsonArray.forEach(json -> extendConfigList.add(JSONUtil.toBean((JSONObject) json, ConnectionExtendConfigInfo.class)));
            if (extendConfigList.size() != 0) {
                connectionBaseInfo.setExtendConfigList(extendConfigList);
            }
        }

        ConnectionAdapteService connectionAdapteService = this.getService(connection.getType());
        if (null == connectionAdapteService) {
            return connectionBaseInfo;
        }
        return connectionAdapteService.decode(connection.getConfig(), connectionBaseInfo);
    }

    /**
     * 连接测试
     * @param type 连接类型
     * @param connectionInfo 连接信息
     * @return
     */
    public ConnectionBaseInfo test(String type, String connectionInfo) {
        if (StrUtil.isEmpty(connectionInfo)) {
            return null;
        }
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return null;
        }
        return connectionAdapteService.test(connectionInfo);
    }

    /**
     * 连接同步，用于同步各数据源缓存
     * @param type 数据源类型
     * @param connectionBaseInfoList 连接信息集合
     */
    public void sync(String type, List<ConnectionBaseInfo> connectionBaseInfoList) {
        ConnectionAdapteService connectionAdapteService = this.getService(type);
        if (null == connectionAdapteService) {
            return;
        }
        connectionAdapteService.sync(connectionBaseInfoList);
    }

    protected Class getClazz() {
        return ConnectionAdapteService.class;
    }

    /**
     * 从Spring容器中获取服务
     * @param type
     * @return
     */
    private ConnectionAdapteService getService(String type) {
        ConnectionAdapteService connectionAdapteService = null;
        if ("db".equals(type)) {
            connectionAdapteService = SpringUtil.getBean(DbConnectionAdapteServiceImpl.class);
        }
        return connectionAdapteService;
    }
}
