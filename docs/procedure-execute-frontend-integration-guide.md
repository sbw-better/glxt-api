# 存储过程接口前端调用与执行逻辑说明

## 1. 适用范围

本文说明 `type=2` 存储过程接口的配置、预览、调用、返回和导出规则。普通 SQL 查询接口使用 `type=1`；历史数据 `type` 为空时仍按 SQL 查询处理。

存储过程沿用原有接口配置、权限校验、执行入口和执行日志，不新增单独的调用接口。

## 2. 配置字段

接口主表 `API_SQL_INTERFACE` 新增：

| 字段 | 前端字段 | 说明 |
| --- | --- | --- |
| `PROCEDURE_NAME` | `procedureName` | 存储过程名称，例如 `PROC_NAME`、`PKG_NAME.PROC_NAME` 或 `SCHEMA.PKG_NAME.PROC_NAME` |

过程名称仅允许字母、数字、下划线、点、`$`、`#`。

参数表 `API_SQL_INTERFACE_PARAM` 新增：

| 字段 | 前端字段 | 说明 |
| --- | --- | --- |
| `DIRECTION` | `direction` | 参数方向：`1=IN`、`2=OUT`、`3=INOUT` |
| `JDBC_TYPE` | `jdbcType` | JDBC 绑定类型；数据库、导入导出和执行层均保存字符串值 |
| `ORDER_NO` | `orderNo` | 位置绑定顺序，从 `1` 开始 |

`orderNo` 决定 `{ call PROC(?, ?, ...) }` 中占位符的位置，不按参数 `code` 绑定。同一个接口的 `orderNo` 必须唯一，且应与 Oracle 过程签名的顺序一致。

## 3. 页面配置规则

当 `type=2` 时，展示 `procedureName`，以及参数的：

```text
orderNo / name / code / direction / type / jdbcType / description
```

存储过程不设置也不展示以下 SQL 参数字段：

```text
required / defaultValue / validateType / expression / error
```

这些字段即使旧数据中存在，存储过程执行分支也会忽略：

- 未传入的 IN/INOUT 参数不会自动补默认值。
- 未传入或显式传 `null` 的 IN/INOUT 参数，按对应 JDBC 类型绑定 SQL `NULL`。
- 存储过程不执行原 SQL 接口的正则或表达式校验。

SQL 配置字段 `selectParam`、`fromParam`、`whereParamFixed`、`whereParamChange`、`groupParam`、`orderParam` 等在存储过程页隐藏；建议 `page` 固定为 `0`。

## 4. 参数类型和 JDBC 类型

`type` 是业务展示类型，`jdbcType` 是数据库调用时的绑定类型，两者不能混用。

| Oracle 常见类型 | `type` 建议 | 入库 `jdbcType` | 说明 |
| --- | ---: | --- | --- |
| `VARCHAR2`、`CHAR` | `1` 字符串 | `VARCHAR` | JSON 请求和返回均为字符串 |
| `NUMBER` 整数 | `2` 整型 | `DECIMAL`、`INTEGER` 或 `BIGINT` | Oracle `NUMBER` 未明确精度时优先使用 `DECIMAL` |
| `NUMBER(p,s)` | `3` 浮点数 | `DECIMAL` | JSON 可传数值或数字字符串 |
| `DATE` | `4` 日期 | `DATE` | 输入格式为 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `TIMESTAMP` | `4` 日期 | `TIMESTAMP` | 输入格式为 `yyyy-MM-dd HH:mm:ss` |
| `CLOB` | `1` 字符串 | `CLOB` | 支持 IN、OUT、INOUT；JSON 中始终是字符串 |
| `SYS_REFCURSOR` | `5` 列表 | `CURSOR` | 仅支持 OUT，返回到 `cursors` |

### 4.1 JDBC 类型下拉值

前端下拉可使用数字 value，保存时接口会把数字转换成标准字符串。配置查询、Excel 导入导出和执行层始终使用右列字符串。

| 下拉 value | `jdbcType` 字符串 |
| ---: | --- |
| `1` | `VARCHAR` |
| `2` | `INTEGER` |
| `3` | `BIGINT` |
| `4` | `DECIMAL` |
| `5` | `DATE` |
| `6` | `TIMESTAMP` |
| `7` | `CLOB` |
| `8` | `CURSOR` |

保存新配置时可以提交：

```json
{ "jdbcType": 4 }
```

后端会保存为：

```json
{ "jdbcType": "DECIMAL" }
```

编辑已有配置时，接口返回的是字符串，前端应按上表反向映射为下拉数字。Excel 导入文件中的“存储过程 JDBC 类型”列填字符串，例如 `DECIMAL` 或 `CLOB`，不要填下拉数字。

`CURSOR` 被选中时，前端应自动将 `direction` 设为 `OUT` 并禁用方向修改，同时将 `type` 设为列表。

## 5. 保存与预览

### 5.1 保存接口

```http
POST /api/interface/save
```

```json
{
  "tenant": "glxt",
  "name": "员工查询",
  "code": "procEmpQuery",
  "type": 2,
  "connectionId": 1001,
  "fieldBackMode": 1,
  "page": 0,
  "procedureName": "PKG_EMP.QUERY_EMP",
  "apiParamList": [
    { "name": "部门 ID", "code": "p_dept_id", "type": 2, "direction": 1, "jdbcType": 4, "orderNo": 1 },
    { "name": "状态码", "code": "p_status", "type": 1, "direction": 2, "jdbcType": 1, "orderNo": 2 },
    { "name": "员工列表", "code": "p_data", "type": 5, "direction": 2, "jdbcType": 8, "orderNo": 3 }
  ]
}
```

保存时校验 `procedureName`、`connectionId`、参数的 `name`、`code`、`type`、`direction`、`jdbcType`、`orderNo`；参数 `code` 不能重复，`orderNo` 必须从 `1` 开始连续编号，`CURSOR` 只能配置为 OUT 且业务参数类型必须为列表。

### 5.2 调用预览

```http
POST /api/interface/preview
```

预览只根据本次页面配置生成绑定结构，不读取已保存接口配置，因此不需要传 `tenant`、`connectionId`、`token` 或 `systemCode`。前端直接提交 JDBC 下拉数值即可，后端会在预览文本中转换为 JDBC 字符串。

```json
{
  "type": 2,
  "procedureName": "PKG_EMP.QUERY_EMP",
  "apiParamList": [
    { "code": "p_dept_id", "direction": 1, "jdbcType": 4, "orderNo": 1 },
    { "code": "p_data", "direction": 2, "jdbcType": 8, "orderNo": 2 }
  ]
}
```

```text
{ call PKG_EMP.QUERY_EMP(? /* p_dept_id:IN:DECIMAL */, ? /* p_data:OUT:CURSOR */) }
```

预览只反映绑定结构，不校验数据库中过程是否存在。

## 6. 执行接口

执行和 Excel 导出均使用：

```http
POST /api/actuator/execute
```

前端不需要传 `type` 或 `procedureName`。后端依据已保存的 `tenant + apiCode` 配置决定执行 SQL 或调用存储过程。

```json
{
  "tenant": "glxt",
  "apiCode": "procEmpQuery",
  "token": "接口分组 token",
  "systemCode": "glxt-web",
  "params": { "p_dept_id": 10 }
}
```

执行参数规则：

- `params` 只能传已配置的 IN、INOUT 参数；传 OUT 或未知参数会报错。
- OUT 参数不渲染输入框。
- IN、INOUT 参数可以省略或传 `null`；后端按其 JDBC 类型绑定 SQL `NULL`。
- 非空 IN、INOUT 参数会按 JDBC 类型转换：`INTEGER`、`BIGINT`、`DECIMAL` 转数值，`DATE`、`TIMESTAMP` 转日期时间，`VARCHAR`、`CLOB` 必须是字符串。
- `CLOB` 的长文本通过字符流绑定；前端无需处理数据库 CLOB 对象。

## 7. 返回结构与页面展示

```json
{
  "outParams": {
    "p_status": "0",
    "p_message": "success"
  },
  "cursors": {
    "p_data": [
      { "emp_id": 1, "emp_name": "张三" }
    ]
  },
  "resultCount": 1
}
```

| 字段 | 内容 | 页面建议 |
| --- | --- | --- |
| `outParams` | 普通 OUT、INOUT 标量，包括 CLOB | 键值区；CLOB 按字符串显示或下载 |
| `cursors` | OUT `SYS_REFCURSOR` 结果集 | 每个 key 一个表格；多游标使用 Tab |
| `resultCount` | 所有游标行数合计 | 展示为统计值 |

即使游标只返回一行，仍返回数组；当前不会自动转换为单个对象。Oracle 自定义 OBJECT、集合类型不在当前支持范围内。

## 8. Excel 导出

在执行请求中加入：

```json
{ "exportExcel": true }
```

| 返回情况 | 导出行为 |
| --- | --- |
| 单个游标 | 导出游标列表 |
| 无游标 | 导出 `outParams` 的 `name/value` 键值表 |
| 多个游标 | 返回“存储过程多游标结果暂不支持Excel导出” |

普通执行支持渲染多个游标；多 Sheet 导出尚未实现。

## 9. 与 SQL 查询接口的关系

- SQL 查询保存、参数必填与业务校验、分页、执行和导出逻辑保持不变。
- 数字 `jdbcType` 到字符串的转换仅发生在 `type=2` 的存储过程保存流程。
- SQL 参数仍使用原有的 `type/required/defaultValue/validateType/expression/error` 配置。

## 10. 联调清单

1. 保存带 IN、OUT、INOUT 的过程，确认数字下拉值入库后为 JDBC 字符串。
2. 使用 `NUMBER` + `DECIMAL`、`VARCHAR2` + `VARCHAR`、`DATE` + `DATE` 联调。
3. 验证 OUT `SYS_REFCURSOR` 返回到 `cursors`。
4. 验证 OUT/INOUT `CLOB` 返回普通字符串，及 IN `CLOB` 的长文本传入。
5. 验证未传、显式 `null` 的 IN/INOUT 参数可调用并绑定 SQL `NULL`。
6. 验证 OUT 或未知参数被传入时返回参数错误。
7. 验证单游标、无游标和多游标的 Excel 导出行为。

可使用以下文件联调：

- [CLOB 测试过程](clob-procedure-test.sql)
- [CLOB 配置说明](clob-procedure-guide.md)
- [员工过程调用示例](employees-procedure-api-test.http)
