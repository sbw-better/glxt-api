package com.citics.glxtapi.web.support;

public class DsContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setDs(String tenant) {
        CONTEXT.set(tenant);
    }

    public static String getDs() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
