package com.citics.glxtapi.plugin.sql.parse;

import com.citics.glxtapi.web.support.SqlContextHolder;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TextSqlNode extends SqlNode {

    private static final GenericTokenParser CONCAT_TOKEN_PARSER = new GenericTokenParser("${", "}", false);
    private static final GenericTokenParser REPLACE_TOKEN_PARSER = new GenericTokenParser("#{", "}", true);
    private static final GenericTokenParser IF_TOKEN_PARSER = new GenericTokenParser("?{", "}", true);
    private static final GenericTokenParser IF_PARAM_TOKEN_PARSER = new GenericTokenParser("?{", ",", true);
    private static final GenericTokenParser IF_AND_TOKEN_PARSER = new GenericTokenParser("?AND{", "}", true);
    private static final GenericTokenParser IF_AND_PARAM_TOKEN_PARSER = new GenericTokenParser("?AND{", ",", true);
    private static final GenericTokenParser IF_OR_TOKEN_PARSER = new GenericTokenParser("?OR{", "}", true);
    private static final GenericTokenParser IF_OR_PARAM_TOKEN_PARSER = new GenericTokenParser("?OR{", ",", true);

    /**
     * 解决ORACLE IN、NOT IN 超过1000个字符会报错的问题
     */
    private static final GenericTokenParser IN_IN_TOKEN_PARSER = new GenericTokenParser("@IN{", "}", true);
    private static final GenericTokenParser IN_PARAM_IN_TOKEN_PARSER = new GenericTokenParser("@IN{", ",", true);
    private static final GenericTokenParser IN_NIN_TOKEN_PARSER = new GenericTokenParser("@NIN{", "}", true);
    private static final GenericTokenParser IN_PARAM_NIN_TOKEN_PARSER = new GenericTokenParser("@NIN{", ",", true);

    /**
     * 模糊匹配
     */
    private static final GenericTokenParser LIKE_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKE{", "}", true);
    private static final GenericTokenParser LIKE_PARAM_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKE{", ",", true);
    private static final GenericTokenParser LIKE_R_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKER{", "}", true);
    private static final GenericTokenParser LIKE_R_PARAM_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKER{", ",", true);
    private static final GenericTokenParser LIKE_L_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKEL{", "}", true);
    private static final GenericTokenParser LIKE_L_PARAM_REPLACE_TOKEN_PARSER = new GenericTokenParser("@LIKEL{", ",", true);

    /**
     * 解决顺序问题
     */
    private static final GenericTokenParser RE_REPLACE_TOKEN_PARSER = new GenericTokenParser("@RE{", "}", true);

    /**
     * SQL
     */
    private final String text;

    public TextSqlNode(String text) {
        this.text = text;
    }

    public static String parseSql(String sql, Map<String, Object> varMap, List<Object> parameters, boolean saveSql) {
        Map<Integer, Object> orderParamMap = new HashMap<>();
        AtomicInteger orderNo = new AtomicInteger(1);

        // 处理?{}参数
        sql = IF_TOKEN_PARSER.parse(sql.trim(), text -> {
            AtomicBoolean ifTrue = new AtomicBoolean(false);
            String val = IF_PARAM_TOKEN_PARSER.parse("?{" + text, param -> {
                ifTrue.set(isTrue(executeExpression(param, varMap)));
                return null;
            });
            return ifTrue.get() ? " " + val : "";
        });

        // 处理?AND{}参数。20250717前端页面取消，但后台保留逻辑
        sql = IF_AND_TOKEN_PARSER.parse(sql.trim(), text -> {
            AtomicBoolean ifTrue = new AtomicBoolean(false);
            String val = IF_AND_PARAM_TOKEN_PARSER.parse("?AND{" + text, param -> {
                ifTrue.set(isTrue(executeExpression(param, varMap)));
                return null;
            });
            return ifTrue.get() ? " AND " + val : "";
        });

        // 处理?OR{}参数。20250717前端页面取消，但后台保留逻辑
        sql = IF_OR_TOKEN_PARSER.parse(sql.trim(), text -> {
            AtomicBoolean ifTrue = new AtomicBoolean(false);
            String val = IF_OR_PARAM_TOKEN_PARSER.parse("?OR{" + text, param -> {
                ifTrue.set(isTrue(executeExpression(param, varMap)));
                return null;
            });
            return ifTrue.get() ? " OR " + val : "";
        });

        // 处理${}参数
        sql = CONCAT_TOKEN_PARSER.parse(sql, text -> String.valueOf(executeExpression(text, varMap)));

        // 处理@IN{}参数
        sql = IN_IN_TOKEN_PARSER.parse(sql, text -> {
            AtomicReference<String> valueStr = new AtomicReference<>("");
            String val = IN_PARAM_IN_TOKEN_PARSER.parse("@IN{" + text, param -> {
                valueStr.set((String) executeExpression(param, varMap));
                return null;
            });
            if (StringUtils.isEmpty(valueStr.get())) {
                return "1=0";
            }
            String res = "(" + val + " IN (";
            List<String> value = Arrays.stream(valueStr.get().split(";")).collect(Collectors.toList());
            int cnt = 0;
            for (int i = 0; i < value.size(); ++i) {
                if (cnt >= 1000) {
                    cnt = 0;
                    res += ") OR " + val + " IN (";
                }
                if (i > 0 && cnt > 0) {
                    res += ",";
                }
                res += "@RE{" + orderNo.get() + "}";
                orderParamMap.put(orderNo.get(), value.get(i).trim());
                orderNo.set(orderNo.get() + 1);
                ++cnt;
            }
            res += "))";
            return res;
        });

        // 处理@NIN{}参数
        sql = IN_NIN_TOKEN_PARSER.parse(sql, text -> {
            AtomicReference<String> valueStr = new AtomicReference<>("");
            String val = IN_PARAM_NIN_TOKEN_PARSER.parse("@NIN{" + text, param -> {
                valueStr.set((String) executeExpression(param, varMap));
                return null;
            });
            if (StringUtils.isEmpty(valueStr.get())) {
                return "1=0";
            }
            String res = "(" + val + " NOT IN (";
            List<String> value = Arrays.stream(valueStr.get().split(";")).collect(Collectors.toList());
            int cnt = 0;
            for (int i = 0; i < value.size(); ++i) {
                if (cnt >= 1000) {
                    cnt = 0;
                    res += ") AND " + val + " NOT IN (";
                }
                if (i > 0 && cnt > 0) {
                    res += ",";
                }
                res += "@RE{" + orderNo.get() + "}";
                orderParamMap.put(orderNo.get(), value.get(i).trim());
                orderNo.set(orderNo.get() + 1);
                ++cnt;
            }
            res += "))";
            return res;
        });

        // 处理@LIKE{}参数
        sql = LIKE_REPLACE_TOKEN_PARSER.parse(sql, text -> {
            AtomicReference<String> value = new AtomicReference<>("");
            String field = LIKE_PARAM_REPLACE_TOKEN_PARSER.parse("@LIKE{" + text, param -> {
                String va = (String) executeExpression(param, varMap);
                va = "%" + va + "%";
                value.set(va);
                return null;
            });
            orderParamMap.put(orderNo.get(), StringUtils.isEmpty(value.get()) ? null : value.get());
            String res = "(" + field + " LIKE @RE{" + orderNo.get() + "})";
            orderNo.set(orderNo.get() + 1);
            return res;
        });

        // 处理@LIKER{}参数
        sql = LIKE_R_REPLACE_TOKEN_PARSER.parse(sql, text -> {
            AtomicReference<String> value = new AtomicReference<>("");
            String field = LIKE_R_PARAM_REPLACE_TOKEN_PARSER.parse("@LIKER{" + text, param -> {
                String va = (String) executeExpression(param, varMap);
                va = va + "%";
                value.set(va);
                return null;
            });
            orderParamMap.put(orderNo.get(), StringUtils.isEmpty(value.get()) ? null : value.get());
            String res = "(" + field + " LIKE @RE{" + orderNo.get() + "})";
            orderNo.set(orderNo.get() + 1);
            return res;
        });

        // 处理@LIKEL{}参数
        sql = LIKE_L_REPLACE_TOKEN_PARSER.parse(sql, text -> {
            AtomicReference<String> value = new AtomicReference<>("");
            String field = LIKE_L_PARAM_REPLACE_TOKEN_PARSER.parse("@LIKEL{" + text, param -> {
                String va = (String) executeExpression(param, varMap);
                va = "%" + va;
                value.set(va);
                return null;
            });
            orderParamMap.put(orderNo.get(), StringUtils.isEmpty(value.get()) ? null : value.get());
            String res = "(" + field + " LIKE @RE{" + orderNo.get() + "})";
            orderNo.set(orderNo.get() + 1);
            return res;
        });

        // 处理#{}参数
        sql = REPLACE_TOKEN_PARSER.parse(sql, text -> {
            Object value = executeExpression(text, varMap);
            if (value == null) {
                orderParamMap.put(orderNo.get(), null);
                String res = "@RE{" + orderNo.get() + "}";
                orderNo.set(orderNo.get() + 1);
                return res;
            }
            try {
                //对集合自动展开
                List<Object> objects = arrayLikeToList(value);
                String res = "";
                for (Object object : objects) {
                    orderParamMap.put(orderNo.get(), object);
                    res = res == "" ? "@RE{" + orderNo.get() + "}" : ",@RE{" + orderNo.get() + "}";
                    orderNo.set(orderNo.get() + 1);
                }
                return res;
            } catch (Exception e) {
                orderParamMap.put(orderNo.get(), value);
                String res = "@RE{" + orderNo.get() + "}";
                orderNo.set(orderNo.get() + 1);
                return res;
            }
        });

        if (saveSql) {
            String executeSql = sql;
            for (Integer k : orderParamMap.keySet()) {
                executeSql = executeSql.replace("@RE{" + k + "}", String.valueOf(orderParamMap.get(k)));
            }
            SqlContextHolder.clear();
            SqlContextHolder.setSql(executeSql);
        }

        // 恢复?替换顺序
        sql = RE_REPLACE_TOKEN_PARSER.parse(sql, text -> {
            Object value = orderParamMap.get(Integer.valueOf(text));
            parameters.add(value);
            return "?";
        });

        return sql;
    }

    @Override
    public String getSql(Map<String, Object> paramMap, List<Object> parameters) {
        return parseSql(text, paramMap, parameters, false) + executeChildren(paramMap, parameters).trim();
    }

    public static boolean isTrue(Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Boolean) {
            return (Boolean) object;
        } else if (object instanceof CharSequence) {
            return ((CharSequence) object).length() != 0;
        } else if (object instanceof Collection) {
            return !((Collection<?>) object).isEmpty();
        } else if (object.getClass().isArray()) {
            return Array.getLength(object) > 0;
        } else if (object instanceof Map) {
            return !((Map<?, ?>) object).isEmpty();
        } else {
            return true;
        }
    }

    public static List<Object> arrayLikeToList(Object arrayLike) {
        if (arrayLike == null) {
            return new ArrayList<>();
        } else if (arrayLike instanceof Collection) {
            return new ArrayList<>((Collection<?>) arrayLike);
        } else {
            List<Object> list;
            if (arrayLike.getClass().isArray()) {
                list = new ArrayList<>(Array.getLength(arrayLike));
                IntStream.range(0, Array.getLength(arrayLike)).forEach(i -> {
                    list.add(Array.get(arrayLike, i));
                });
                return list;
            } else if (arrayLike instanceof Iterator) {
                list = new ArrayList<>();
                Iterator<Object> it = (Iterator<Object>) arrayLike;
                it.forEachRemaining(list::add);
                return list;
            } else if (arrayLike instanceof Enumeration) {
                Enumeration<Object> en = (Enumeration<Object>) arrayLike;
                return Collections.list(en);
            } else {
                throw new RuntimeException("不支持的类型:" + arrayLike.getClass());
            }
        }
    }

    /**
     * 执行脚本
     */
    public static Object executeExpression(String script, Map<String, Object> paramMap) {
        return paramMap.containsKey(script) ? paramMap.get(script) : null;
    }
}
