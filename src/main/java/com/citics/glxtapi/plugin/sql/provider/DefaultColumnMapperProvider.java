package com.citics.glxtapi.plugin.sql.provider;

/**
 * 默认命名（保持原样）
 */
public class DefaultColumnMapperProvider implements ColumnMapperProvider {

    @Override
    public String name() {
        return "default";
    }

    @Override
    public String mapping(String columnName) {
        return columnName;
    }
}
