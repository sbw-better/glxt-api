package com.citics.glxtapi.plugin.db.creator;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.logging.Slf4jLogFilter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.citics.glxtapi.plugin.db.config.druid.DruidConfig;
import com.citics.glxtapi.plugin.db.config.druid.DruidSlf4jConfig;
import com.citics.glxtapi.plugin.db.config.druid.DruidWallConfigUtil;
import com.citics.glxtapi.plugin.db.domain.DataSourceProperty;
import com.citics.glxtapi.plugin.db.exception.ErrorCreateDataSourceException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Druid数据源创建器
 */
@Data
@Slf4j
public class DruidDataSourceCreator {

    private DruidConfig druidConfig;

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    public DruidDataSourceCreator(DruidConfig druidConfig) {
        this.druidConfig = druidConfig;
    }

    public DataSource createDataSource(DataSourceProperty dataSourceProperty) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUsername(dataSourceProperty.getUsername());
        dataSource.setPassword(dataSourceProperty.getPassword());
        dataSource.setUrl(dataSourceProperty.getUrl());
        dataSource.setName(dataSourceProperty.getPoolName());
        String driverClassName = dataSourceProperty.getDriverClassName();
        if (!StringUtils.isEmpty(driverClassName)) {
            dataSource.setDriverClassName(driverClassName);
        }

        DruidConfig config = dataSourceProperty.getDruid();
        Properties properties = config.toProperties(druidConfig);
        String filters = properties.getProperty("druid.filters");
        List<Filter> proxyFilters = new ArrayList<>(2);

        if (!StringUtils.isEmpty(filters) && filters.contains("stat")) {
            StatFilter statFilter = new StatFilter();
            statFilter.configFromProperties(properties);
            proxyFilters.add(statFilter);
        }

        if (!StringUtils.isEmpty(filters) && filters.contains("wall")) {
            WallConfig wallConfig = DruidWallConfigUtil.toWallConfig(dataSourceProperty.getDruid().getWall(), druidConfig.getWall());
            WallFilter wallFilter = new WallFilter();
            wallFilter.setConfig(wallConfig);
            proxyFilters.add(wallFilter);
        }

        if (!StringUtils.isEmpty(filters) && filters.contains("slf4j")) {
            Slf4jLogFilter slf4jLogFilter = new Slf4jLogFilter();
            DruidSlf4jConfig slf4jConfig = druidConfig.getSlf4j();
            slf4jLogFilter.setStatementLogEnabled(slf4jConfig.getEnable());
            slf4jLogFilter.setStatementExecutableSqlLogEnable(slf4jConfig.getStatementExecutableSqlLogEnable());
            proxyFilters.add(slf4jLogFilter);
        }

        if (this.applicationContext != null) {
            for (String filterId : druidConfig.getProxyFilters()) {
                proxyFilters.add(this.applicationContext.getBean(filterId, Filter.class));
            }
        }

        // ===== 新增DMs数据库兼容处理 =====
        boolean isDm = dataSourceProperty.getUrl().contains("jdbc:dm:");
        List<Filter> filtersToRemove = new ArrayList<>();
        if (isDm) {
            // 识别并移除所有WallFilter
            for (Filter filter : proxyFilters) {
                if (filter instanceof WallFilter) {
                    filtersToRemove.add(filter);
                    log.warn("移除了WallFilter以兼容DM数据库");
                }
            }
            proxyFilters.removeAll(filtersToRemove);
        }

        if (isDm && !filtersToRemove.isEmpty()) {
            // 禁用防火墙相关配置
            properties.remove("druid.wall.enabled");
            properties.setProperty("druid.filters",
                    properties.getProperty("druid.filters", "").replace("wall", ""));
        }

        dataSource.setProxyFilters(proxyFilters);
        dataSource.configFromPropeties(properties);

        //连接参数单独设置
        dataSource.setConnectProperties(config.getConnectionProperties());

        //设置druid内置properties不支持的的参数
        Boolean testOnReturn = config.getTestOnReturn() == null ? druidConfig.getTestOnReturn() : config.getTestOnReturn();
        if (testOnReturn != null && testOnReturn.equals(true)) {
            dataSource.setTestOnReturn(true);
        }

        Integer validationQueryTimeout = config.getValidationQueryTimeout() == null ? druidConfig.getValidationQueryTimeout() : config.getValidationQueryTimeout();
        if (validationQueryTimeout != null && !validationQueryTimeout.equals(-1)) {
            dataSource.setValidationQueryTimeout(validationQueryTimeout);
        }

        Boolean sharePreparedStatements = config.getSharePreparedStatements() == null ? druidConfig.getSharePreparedStatements() : config.getSharePreparedStatements();
        if (sharePreparedStatements != null && sharePreparedStatements.equals(true)) {
            dataSource.setSharePreparedStatements(true);
        }

        Integer connectionErrorRetryAttempts = config.getConnectionErrorRetryAttempts() == null ? druidConfig.getConnectionErrorRetryAttempts()
                : config.getConnectionErrorRetryAttempts();
        if (connectionErrorRetryAttempts != null && !connectionErrorRetryAttempts.equals(1)) {
            dataSource.setConnectionErrorRetryAttempts(connectionErrorRetryAttempts);
        }

        Boolean breakAfterAcquireFailure = config.getBreakAfterAcquireFailure() == null ? druidConfig.getBreakAfterAcquireFailure() : config.getBreakAfterAcquireFailure();
        if (breakAfterAcquireFailure != null && breakAfterAcquireFailure.equals(true)) {
            dataSource.setBreakAfterAcquireFailure(true);
        }

        Integer timeout = config.getRemoveAbandonedTimeoutMillis() == null ? druidConfig.getRemoveAbandonedTimeoutMillis()
                : config.getRemoveAbandonedTimeoutMillis();
        if (timeout != null) {
            dataSource.setRemoveAbandonedTimeoutMillis(timeout);
        }

        Boolean abandoned = config.getRemoveAbandoned() == null ? druidConfig.getRemoveAbandoned() : config.getRemoveAbandoned();
        if (abandoned != null) {
            dataSource.setRemoveAbandoned(abandoned);
        }

        Boolean logAbandoned = config.getLogAbandoned() == null ? druidConfig.getLogAbandoned() : config.getLogAbandoned();
        if (logAbandoned != null) {
            dataSource.setLogAbandoned(logAbandoned);
        }

        Integer queryTimeOut = config.getQueryTimeout() == null ? druidConfig.getQueryTimeout() : config.getQueryTimeout();
        if (queryTimeOut != null) {
            dataSource.setQueryTimeout(queryTimeOut);
        }

        Integer transactionQueryTimeout = config.getTransactionQueryTimeout() == null ? druidConfig.getTransactionQueryTimeout() : config.getTransactionQueryTimeout();
        if (transactionQueryTimeout != null) {
            dataSource.setTransactionQueryTimeout(transactionQueryTimeout);
        }

        dataSource.setSocketTimeout(-1);

        try {
            dataSource.init();
        } catch (SQLException e) {
            throw new ErrorCreateDataSourceException("druid create error", e);
        }
        return dataSource;
    }
}