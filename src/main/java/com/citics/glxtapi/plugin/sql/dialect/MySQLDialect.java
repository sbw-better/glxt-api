package com.citics.glxtapi.plugin.sql.dialect;


import com.citics.glxtapi.plugin.sql.BoundSql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * mysql 方言
 */
public class MySQLDialect implements Dialect {

    @Override
    public boolean match(DatabaseMetaData jdbcMeta) throws SQLException {
        return jdbcMeta.getURL().contains(":mysql:")
                || jdbcMeta.getURL().contains(":mariadb:")
                || jdbcMeta.getURL().contains(":cobar:");
    }

    @Override
    public String getPageSql(String sql, BoundSql boundSql, long offset, long limit) {
        boundSql.addParameter(offset);
        boundSql.addParameter(limit);
        return sql + "\n limit ?,?";
    }
}
