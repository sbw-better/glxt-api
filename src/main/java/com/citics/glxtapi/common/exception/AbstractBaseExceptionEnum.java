package com.citics.glxtapi.common.exception;

/**
 * 异常枚举格式规范
 */
public interface AbstractBaseExceptionEnum {

    /**
     * 获取异常的状态码
     */
    Integer getCode();

    /**
     * 获取异常的提示信息
     */
    String getMessage();

}
