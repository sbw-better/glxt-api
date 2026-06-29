package com.citics.glxtapi.web.model;

import com.citics.glxtapi.common.utils.string.ObjectConvertExtension;

import java.util.function.Function;

/**
 * 参数类型枚举
 */
public enum DataType {

    /**
     * Object 类型
     */
    Object("object"),

    /**
     * 基础类型数组
     */
    Array("array"),

    /**
     * 数组类型
     */
    String("string"),

    /**
     * Boolean类型
     */
    Boolean("boolean"),

    /**
     * Date类型
     */
    Date("date", v -> ObjectConvertExtension.asDate(v, "yyyy-MM-dd")),

    /**
     * DateTime类型
     */
    DateTime("dateTime", v -> ObjectConvertExtension.asDate(v, "yyyy-MM-dd HH:mm:ss")),

    /**
     * Integer 类型
     */
    Integer("integer", true, v -> ObjectConvertExtension.getInstance().asInt(v)),

    /**
     * Integer 类型
     */
    Int("int", true, v -> ObjectConvertExtension.getInstance().asInt(v)),

    /**
     * Double 类型
     */
    Double("double", true, v -> ObjectConvertExtension.getInstance().asDouble(v)),

    /**
     * Long 类型
     */
    Long("long", true, v -> ObjectConvertExtension.getInstance().asLong(v)),

    /**
     * Short类型
     */
    Short("short", true, v -> ObjectConvertExtension.getInstance().asShort(v)),

    /**
     * Float 类型
     */
    Float("float", true, v -> ObjectConvertExtension.getInstance().asFloat(v)),

    /**
     * MultipartFile 类型
     */
    MultipartFile("multipartFile"),

    /**
     * MultipartFiles 类型
     */
    MultipartFiles("multipartFiles"),

    /**
     * 任意类型
     */
    Any("any");

    private String value;
    private boolean isNumber;
    private Function<Object, Object> convert;

    DataType(String value) {
        this.value = value;
    }

    DataType(String value, Function<Object, Object> convert) {
        this.value = value;
        this.convert = convert;
    }

    DataType(String value, boolean isNumber) {
        this.value = value;
        this.isNumber = isNumber;
    }

    DataType(String value, boolean isNumber, Function<Object, Object> convert) {
        this.value = value;
        this.isNumber = isNumber;
        this.convert = convert;
    }

    public boolean isNumber() {
        return isNumber;
    }

    public String getValue() {
        return value;
    }

    public Function<Object, Object> getConvert() {
        return convert;
    }
}
