package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.plugin.db.domain.ConnectionBaseInfo;
import com.citics.glxtapi.plugin.db.domain.DbConnectionInfo;
import com.citics.glxtapi.web.entity.Connection;

import java.io.Serializable;
import java.util.List;

public interface ConnectionService extends IService<Connection> {

    /**
     * 测试连接
     * @param dto
     * @return
     */
    ConnectionBaseInfo test(DbConnectionInfo dto);

    /**
     * 保存连接
     * @param dto
     * @return
     */
    ConnectionBaseInfo save(DbConnectionInfo dto);

    /**
     * 删除
     * @param id id
     * @return
     */
    boolean delete(Serializable id);

    /**
     * 获取指定类型连接
     * @param type
     * @return
     */
    List<ConnectionBaseInfo> list(String type);

    /**
     * 获取指定类型连接
     * @param dto
     * @return
     */
    PageResult page(DbConnectionInfo dto);

    /**
     * 获取指定类型连接
     * @param type 类型
     * @return
     */
    List<ConnectionBaseInfo> listAll(String type);

    /**
     * 程序启动时，初始化连接
     * @return
     */
    Boolean initialize();

    /**
     * 同步所有数据源连接
     * @return
     */
    Boolean sync();

    /**
     * 判断租户是否存在此连接权限
     * @param tenant
     * @param connectionId
     * @return
     */
    Boolean isHaveConnectionPermission(String tenant, Long connectionId);

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    Boolean isHaveTenant(String tenant);
}
