package com.citics.glxtapi.web.support;

/**
 * 记录执行的SQL语句
 */
public class SqlContextHolder {

    // sql执行语句
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    // sql执行结果数量
    private static final ThreadLocal<Integer> CONTEXT_COUNT = new ThreadLocal<>();

    public static void setSql(String sql) {
        CONTEXT.set(sql);
    }

    public static String getSql() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void setSqlCount(Integer sqlCount) {
        CONTEXT_COUNT.set(sqlCount);
    }

    public static Integer getSqlCount() {
        return CONTEXT_COUNT.get();
    }

    public static void clearCount() {
        CONTEXT_COUNT.remove();
    }
}
