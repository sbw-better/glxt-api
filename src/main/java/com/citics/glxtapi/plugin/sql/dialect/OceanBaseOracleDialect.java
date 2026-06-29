package com.citics.glxtapi.plugin.sql.dialect;


import com.citics.glxtapi.plugin.sql.BoundSql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Oracle方言
 */
public class OceanBaseOracleDialect implements Dialect {

    @Override
    public boolean match(DatabaseMetaData jdbcMeta) throws SQLException {
        return jdbcMeta.getURL().contains(":oceanbase:") && jdbcMeta.getDatabaseProductName().contains("Oracle");
    }

    @Override
    public String getPageSql(String sql, BoundSql boundSql, long offset, long limit) {
        limit = (offset >= 1) ? (offset + limit) : limit;
        boundSql.addParameter(limit);
        boundSql.addParameter(offset);
        return "SELECT * FROM ( SELECT TMP.*, ROWNUM EXTRA_ROW_ID FROM (\n" +
                sql + "\n ) TMP WHERE ROWNUM <= ? ) WHERE EXTRA_ROW_ID > ?";
    }
}
