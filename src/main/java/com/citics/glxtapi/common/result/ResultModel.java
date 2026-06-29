package com.citics.glxtapi.common.result;


import com.citics.glxtapi.common.constants.CommonConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果封装
 */
@Data
public class ResultModel<T> implements Serializable {

    private static final long serialVersionUID = -6104902776087367038L;

    private Integer code;
    private String message;
    private T data = null;

    public ResultModel() {
    }

    public boolean isSuccess() {
        return null != code && CommonConstants.SUCCESS == code;
    }

    public static <T> ResultModel<T> setResult(Integer code, String msg, T data) {
        ResultModel<T> resultModel = new ResultModel<>();
        resultModel.setCode(code);
        resultModel.setMessage(msg);
        resultModel.setData(data);
        return resultModel;
    }

    /**
     * 成功无参
     */
    public static <T> ResultModel<T> success() {
        return setResult(CommonConstants.SUCCESS, CommonConstants.SUCCESSMSG, null);
    }

    /**
     * 成功带数据
     */
    public static <T> ResultModel<T> success(T data) {
        return setResult(CommonConstants.SUCCESS, CommonConstants.SUCCESSMSG, data);
    }

    /**
     * 成功自定义消息+数据
     */
    public static <T> ResultModel<T> success(String msg, T data) {
        return setResult(CommonConstants.SUCCESS, msg, data);
    }

    /**
     * 失败默认
     */
    public static <T> ResultModel<T> error() {
        return setResult(CommonConstants.SERVER_FAIL, CommonConstants.FAILMSG, null);
    }

    /**
     * 失败自定义消息
     */
    public static <T> ResultModel<T> error(String msg) {
        return setResult(CommonConstants.SERVER_FAIL, msg, null);
    }

    /**
     * 失败带数据
     */
    public static <T> ResultModel<T> error(T data) {
        return setResult(CommonConstants.SERVER_FAIL, CommonConstants.FAILMSG, data);
    }

    /**
     * 失败自定义消息+数据
     */
    public static <T> ResultModel<T> error(String msg, T data) {
        return setResult(CommonConstants.SERVER_FAIL, msg, data);
    }

}