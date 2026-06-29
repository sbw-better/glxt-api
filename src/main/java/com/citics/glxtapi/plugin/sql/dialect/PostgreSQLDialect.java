package com.citics.glxtapi.plugin.sql.dialect;


import com.citics.glxtapi.plugin.sql.BoundSql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * PostgreSQL 方言
 */
public class PostgreSQLDialect implements Dialect {

    @Override
    public boolean match(DatabaseMetaData jdbcMeta) throws SQLException {
        return jdbcMeta.getURL().contains(":postgresql:") || jdbcMeta.getURL().contains(":greenplum:");
    }

    @Override
    public String getPageSql(String sql, BoundSql boundSql, long offset, long limit) {
        boundSql.addParameter(limit);
        boundSql.addParameter(offset);
        return sql + "\n limit ? offset ?";
    }
}
