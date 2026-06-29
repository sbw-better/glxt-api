package com.citics.glxtapi.plugin.sql.interceptor;

import com.citics.glxtapi.plugin.sql.BoundSql;
import com.citics.glxtapi.web.context.RequestEntity;

/**
 * SQL 拦截器
 */
public interface SQLInterceptor {

    /**
     * 1.1.1 新增
     *
     * @since 1.1.1
     * @param boundSql SQL信息
     * @param requestEntity 请求信息
     */
    default void preHandle(BoundSql boundSql, RequestEntity requestEntity) {

    }

    /**
     * @since 1.7.2
     * @param boundSql SQL信息
     * @param result 执行结果
     * @param requestEntity 请求信息
     */
    default Object postHandle(BoundSql boundSql, Object result, RequestEntity requestEntity) {
        return result;
    }
}