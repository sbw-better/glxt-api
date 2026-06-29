package com.citics.glxtapi.plugin.sql.provider;

/**
 * 全大写命名
 */
public class UpperColumnMapperProvider implements ColumnMapperProvider {

    @Override
    public String name() {
        return "upper";
    }

    @Override
    public String mapping(String columnName) {
        return columnName == null ? null : columnName.toUpperCase();
    }
}