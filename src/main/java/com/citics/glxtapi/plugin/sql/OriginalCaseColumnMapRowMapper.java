package com.citics.glxtapi.plugin.sql;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * 支持查询时按照配置的别名，字母大小写原样返回
 * </p>
 *
 * @author wuxingkun
 * @since 2026-04-23
 */
public class OriginalCaseColumnMapRowMapper implements RowMapper<Map<String, Object>> {

    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 保留数据库返回列顺序，后续动态Excel导出表头会按Map顺序生成。
        Map<String, Object> map = new LinkedHashMap<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            // 获取列别名（就是 AS "xxx" 里的 xxx，原样返回）
            String columnLabel = metaData.getColumnLabel(i);
            Object value = JdbcUtils.getResultSetValue(rs, i);
            map.put(columnLabel, value);
        }

        return map;
    }
}
