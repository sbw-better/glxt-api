package com.citics.glxtapi.plugin.sql;

import com.citics.glxtapi.plugin.sql.parse.TextSqlNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Supplier;

public class BoundSql {

    private static final Pattern REPLACE_MULTI_WHITE_LINE = Pattern.compile("(\\r?\\n\\s*\\n)+");

    private String sql;

    private List<Object> parameters = new ArrayList<>();

    private Set<String> excludeColumns;

    private DbModule dbModule;

    private Map<String, Object> bindParameters;

    private boolean saveSql;

    private Integer fieldBackMode;

    public BoundSql(String sql, List<Object> parameters, DbModule dbModule) {
        this.sql = sql;
        this.parameters = parameters;
        this.dbModule = dbModule;
    }

    public BoundSql(String sql, Map<String, Object> parameters, DbModule dbModule, boolean saveSql, Integer fieldBackMode) {
        this.sql = sql;
        this.bindParameters = parameters;
        this.dbModule = dbModule;
        this.saveSql = saveSql;
        this.fieldBackMode = fieldBackMode;
        this.init();
    }

    private BoundSql(String sql) {
        this.sql = sql;
        this.init();
    }

    BoundSql(String sql, DbModule dbModule) {
        this(sql);
        this.dbModule = dbModule;
    }

    private BoundSql() {

    }

    private void init() {
        Map<String, Object> varMap = new HashMap<>();
        if (null != this.bindParameters) {
            varMap.putAll(this.bindParameters);
        }
        normal(varMap);
    }

    private void normal(Map<String, Object> varMap) {
        this.sql = TextSqlNode.parseSql(this.sql, varMap, parameters, saveSql);
        if (this.sql != null) {
            this.sql = REPLACE_MULTI_WHITE_LINE.matcher(this.sql.trim()).replaceAll("\r\n");
        }
    }

    public DbModule getDbModule() {
        return dbModule;
    }

    BoundSql copy(String newSql) {
        BoundSql boundSql = new BoundSql();
        boundSql.parameters = this.parameters;
        boundSql.bindParameters = this.bindParameters;
        boundSql.sql = newSql;
        boundSql.excludeColumns = this.excludeColumns;
        boundSql.dbModule = this.dbModule;
        return boundSql;
    }

    public Set<String> getExcludeColumns() {
        return excludeColumns;
    }

    public void setExcludeColumns(Set<String> excludeColumns) {
        this.excludeColumns = excludeColumns;
    }

    /**
     * 添加SQL参数
     */
    public void addParameter(Object value) {
        parameters.add(value);
    }

    /**
     * 获取要执行的SQL
     */
    public String getSql() {
        return sql;
    }

    /**
     * 设置要执行的SQL
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * 获取要执行的参数
     */
    public Object[] getParameters() {
        return parameters.toArray();
    }

    /**
     * 获取字段返回模式
     */
    public Integer getFieldBackMode() {
        return fieldBackMode;
    }

    /**
     * 设置要执行的参数
     */
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * 获取缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(Supplier<T> supplier) {
        Object result = supplier.get();
        return (T) result;
    }
}
