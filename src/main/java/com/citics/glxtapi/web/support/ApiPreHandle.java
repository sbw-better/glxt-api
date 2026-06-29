package com.citics.glxtapi.web.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 预处理
 *
 * @author citics
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiPreHandle {

    /**
     * 描述
     */
    String value() default "";

    /**
     * 允许调用的最低用户角色
     */
    String minAccessRole() default "";
}
