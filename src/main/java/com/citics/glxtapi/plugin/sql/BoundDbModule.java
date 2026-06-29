package com.citics.glxtapi.plugin.sql;

import com.citics.glxtapi.plugin.db.DynamicRoutingDataSource;
import com.citics.glxtapi.plugin.db.support.DynamicDataSourceContextHolder;
import com.citics.glxtapi.plugin.sql.dialect.Dialect;
import com.citics.glxtapi.plugin.sql.dialect.DialectAdapter;
import com.citics.glxtapi.plugin.sql.interceptor.SQLInterceptor;
import com.citics.glxtapi.web.context.RequestContext;
import com.citics.glxtapi.web.entity.interceptor.ResultProvider;
import com.citics.glxtapi.web.exception.APIException;
import com.citics.glxtapi.web.service.TenantService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.beans.Transient;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class BoundDbModule extends DbModule {

    private String dsKey = "";

    public BoundDbModule(DynamicRoutingDataSource dynamicRoutingDataSource, TenantService tenantService,
                         List<SQLInterceptor> sqlInterceptors, DialectAdapter dialectAdapter,
                         ResultProvider resultProvider, ColumnMapperAdapter columnMapperAdapter,
                         RowMapper<Map<String, Object>> columnMapRowMapper, Function<String, String> rowMapColumnMapper,
                         String logicDeleteColumn, String logicDeleteValue,
                         String dsKey) {
        super(null, dynamicRoutingDataSource, tenantService);
        this.setSqlInterceptors(sqlInterceptors);
        this.setDialectAdapter(dialectAdapter);
        this.setResultProvider(resultProvider);
        this.setColumnMapperProvider(columnMapperAdapter);
        this.setColumnMapRowMapper(columnMapRowMapper);
        this.setRowMapColumnMapper(rowMapColumnMapper);
        this.setLogicDeleteColumn(logicDeleteColumn);
        this.setLogicDeleteValue(logicDeleteValue);
        this.dsKey = dsKey;
        DataSource dataSource = dynamicRoutingDataSource.getDataSource(dsKey);
        this.setJdbcTemplate(new JdbcTemplate(dataSource));
    }

    /**
     * 开启事务，手动提交和回滚
     *
     * @return
     */
    @Override
    public Transaction transaction() {
        try {
            return new Transaction(new DataSourceTransactionManager(this.jdbcTemplate.getDataSource()));
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(BoundSql boundSql, Supplier<T> supplier) {
        if (null != boundSql) {
            this.sqlInterceptors.forEach(interceptor -> interceptor.preHandle(boundSql, RequestContext.getRequestEntity()));
        }
        Object result = supplier.get();
        return (T) result;
    }

    @Override
    @Transient
    public DbModule columnCase(String name) {
        this.setColumnMapRowMapper(columnMapperAdapter.getColumnMapRowMapper(name));
        this.setRowMapColumnMapper(columnMapperAdapter.getRowMapColumnMapper(name));
        return this;
    }

    @Override
    @Transient
    public String getDataSourceName() {
        return this.dsKey;
    }

    @Override
    public Dialect getDialect() {
        DataSource dataSource = this.jdbcTemplate.getDataSource();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Dialect dialect = dialectAdapter.getDialectFromConnection(connection);
            if (dialect == null) {
                throw new APIException("自动获取数据库方言失败");
            }
            return dialect;
        } catch (Exception e) {
            throw new APIException("自动获取数据库方言失败", e);
        } finally {
            if (null != connection) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }
}
