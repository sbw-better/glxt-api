package com.citics.glxtapi.plugin.db.creator;

import com.citics.glxtapi.plugin.db.config.hikari.HikariCpConfig;
import com.citics.glxtapi.plugin.db.domain.DataSourceProperty;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;

/**
 * Hikari数据源创建器
 */
@Data
@AllArgsConstructor
public class HikariDataSourceCreator {

    private HikariCpConfig hikariCpConfig;

    public DataSource createDataSource(DataSourceProperty dataSourceProperty) {
        HikariConfig config = dataSourceProperty.getHikari().toHikariConfig(hikariCpConfig);
        config.setUsername(dataSourceProperty.getUsername());
        config.setPassword(dataSourceProperty.getPassword());
        config.setJdbcUrl(dataSourceProperty.getUrl());
        config.setPoolName(dataSourceProperty.getPoolName());
        String driverClassName = dataSourceProperty.getDriverClassName();
        if (!StringUtils.isEmpty(driverClassName)) {
            config.setDriverClassName(driverClassName);
        }
        return new HikariDataSource(config);
    }
}
