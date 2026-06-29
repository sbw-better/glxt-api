package com.citics.glxtapi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义接口字典转换注解
 *
 * @author wangzhe
 * @date 2022-06-30
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dict {

    //数据字典的code @Dict(dictCode = "SF")
    String dictCode() default "";

    //内部对象转换信息 @Dict(objCode = "table=TPIF_CPDM,display=CPQC,key=ID")
    String objCode() default "";

    //字典值类型（单值、多值） single/multi
    String type() default "";

    //字典转换方式 (add/replace/format)
    String convertMode() default "add";

    //字典转换方式为add时，添加的字段名
    String convertAddName() default "";

    //子类字典是否转换 (List / Class)
    String childConvert() default "no";
}