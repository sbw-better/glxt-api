# 存储过程 CLOB 配置

CLOB 支持 IN、OUT、INOUT；jdbcType 提交字符串 CLOB，业务参数类型 type=1。
是否必填、默认值及业务校验字段无需配置。

测试过程见 clob-procedure-test.sql，参数按以下顺序配置：

```json
[
  {"name":"输入文本","code":"p_input","direction":1,"type":1,"jdbcType":"CLOB","orderNo":1},
  {"name":"输出文本","code":"p_output","direction":2,"type":1,"jdbcType":"CLOB","orderNo":2},
  {"name":"输入输出文本","code":"p_inout","direction":3,"type":1,"jdbcType":"CLOB","orderNo":3}
]
```

执行请求的 params 示例：

```json
{"p_input":"中文长文本","p_inout":"原始文本"}
```

返回 outParams.p_output 和 outParams.p_inout 均为字符串“中文长文本”。
未传或显式 null 的输入绑定 SQL NULL；输出 SQL NULL 返回 JSON null。
输出不自动解析成 JSON 对象，即使文本内容本身是 JSON。

数据库 JDBC_TYPE 已为 VARCHAR2(32)，无需更改列类型；只需为前端字典增加 CLOB。
后端改动仅涉及存储过程校验、类型转换和 CallableStatement 调用，不修改普通 SQL 的绑定及结果读取。

验证：使用超过 32767 字符的中文文本调用，比较完整返回内容；再验证 null 和空字符串。Oracle 对空字符串的具体表现需以实际驱动和数据库联调结果为准。
单元测试使用 JDBC mock，不替代 Oracle 11.2.0.4 的实际联调。
