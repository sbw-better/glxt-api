package com.citics.glxtapi.web.support;

/**
 * 租户user，用于记录登录用户信息
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CONTEXT_ROLE = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CONTEXT.set(userId);
    }

    public static Long getUserId() {
        return CONTEXT.get();
    }

    public static void clearUserId() {
        CONTEXT.remove();
    }

    public static void setUserRole(Long userRole) {
        CONTEXT_ROLE.set(userRole);
    }

    public static Long getUserRole() {
        return CONTEXT_ROLE.get();
    }

    public static void clearUserRole() {
        CONTEXT_ROLE.remove();
    }
}
