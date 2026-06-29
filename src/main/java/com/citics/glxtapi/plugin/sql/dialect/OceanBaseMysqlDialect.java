package com.citics.glxtapi.plugin.sql.dialect;


import com.citics.glxtapi.plugin.sql.BoundSql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Oracle方言
 */
public class OceanBaseMysqlDialect implements Dialect {

    @Override
    public boolean match(DatabaseMetaData jdbcMeta) throws SQLException {
        return jdbcMeta.getURL().contains(":oceanbase:") && !jdbcMeta.getDatabaseProductName().contains("Oracle");
    }

    @Override
    public String getPageSql(String sql, BoundSql boundSql, long offset, long limit) {
        boundSql.addParameter(offset);
        boundSql.addParameter(limit);
        return sql + "\n limit ?,?";
    }
}