# 存储过程接口前端调用开发文档

适用项目：`glxt-api`

文档版本：v1.0

更新日期：2026-07-22

## 1. 功能概览

本功能在现有 API 接口配置与执行体系上新增“存储过程接口”类型。前端仍使用原有接口配置、查询、预览、Demo 生成和执行入口；后端根据接口配置中的 `type` 判断执行 SQL 查询还是存储过程。

| 能力 | 接口 | 说明 |
| --- | --- | --- |
| 保存配置 | `POST /api/interface/save` | 新增或更新 SQL 查询接口、存储过程接口 |
| 查询详情 | `POST /api/interface/get` | 查询接口主配置和参数配置 |
| 列表查询 | `POST /api/interface/list` | 不分页查询接口配置 |
| 分页查询 | `POST /api/interface/page` | 分页查询接口配置 |
| SQL/过程预览 | `POST /api/interface/preview` | SQL 类型返回预览 SQL；过程类型返回 `{ call ... }` |
| 调用 Demo | `POST /api/interface/demo` | 生成 `/api/actuator/execute` 请求体示例 |
| 执行接口 | `POST /api/actuator/execute` | 根据接口类型执行 SQL 查询或存储过程 |
| 导出 Excel | `POST /api/actuator/execute` | 请求体传 `exportExcel=true` |
| 执行记录 | `POST /api/actuator/page` | 查询接口调用记录 |

## 2. 类型与枚举

### 2.1 接口类型 `type`

| 值 | 含义 | 说明 |
| --- | --- | --- |
| `1` | SQL 查询接口 | 使用 `selectParam/fromParam/whereParamFixed/...` 拼接 SQL |
| `2` | 存储过程接口 | 使用 `procedureName` 和参数顺序调用存储过程 |

### 2.2 存储过程参数方向 `direction`

| 值 | 含义 | 前端是否需要在执行时传值 |
| --- | --- | --- |
| `1` | IN | 需要，按是否必填和默认值规则处理 |
| `2` | OUT | 不需要，由后端读取返回 |
| `3` | INOUT | 需要传入，同时会作为 OUT 读取返回 |

### 2.3 存储过程 JDBC 类型 `jdbcType`

| 值 | 说明 | 备注 |
| --- | --- | --- |
| `VARCHAR` | 字符串 | 入参执行时传字符串 |
| `INTEGER` | 整型 | 入参执行时可传数字或数字字符串 |
| `BIGINT` | 长整型 | 入参执行时可传数字或数字字符串 |
| `DECIMAL` | 数值 | 入参执行时可传数字或数字字符串 |
| `DATE` | 日期 | 入参格式：`yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `TIMESTAMP` | 时间戳 | 入参格式：`yyyy-MM-dd HH:mm:ss` |
| `CURSOR` | 游标结果集 | 一期仅支持 `OUT` 方向 |

## 3. 配置保存接口

### 3.1 请求信息

| 项 | 值 |
| --- | --- |
| URL | `/api/interface/save` |
| Method | `POST` |
| Content-Type | `application/json` |

### 3.2 存储过程主配置字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 更新时必填 | 新增可不传或传 `-1` |
| `tenant` | string | 按登录/租户上下文 | 租户编码 |
| `name` | string | 是 | 接口名称 |
| `code` | string | 是 | 接口编码，执行时作为 `apiCode` |
| `type` | number | 是 | 存储过程传 `2` |
| `description` | string | 否 | 接口说明 |
| `orderNo` | number | 否 | 排序号 |
| `connectionId` | number | 是 | 数据源 ID |
| `procedureName` | string | 是 | 存储过程名称，如 `PKG_FUND.QUERY_LIST` |
| `fieldBackMode` | number | 否 | 游标字段返回模式；`1` 默认小写，`2` 按别名原样 |
| `apiParamList` | array | 是 | 存储过程参数配置 |

说明：

- 存储过程接口不需要配置 `selectParam`、`fromParam`、`whereParamFixed`、`groupParam`、`orderParam`。
- `procedureName` 只允许配置端保存，执行时调用方不能动态传过程名。
- 后端会按 `apiParamList[*].orderNo` 生成 `{ call procedureName(?, ?, ...) }`。

### 3.3 存储过程参数字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 更新旧参数时必填 | 新增参数可不传 |
| `name` | string | 是 | 参数名称，用于页面展示 |
| `code` | string | 是 | 参数编码，执行入参和返回结果使用该 key |
| `direction` | number | 是 | `1=IN`、`2=OUT`、`3=INOUT` |
| `jdbcType` | string | 是 | 见 JDBC 类型枚举 |
| `orderNo` | number | 是 | 参数在存储过程签名中的位置，从 `1` 开始 |
| `required` | number | IN/INOUT 必填 | `1=必填`、`0=非必填`；OUT 参数可不传 |
| `defaultValue` | string | 否 | IN/INOUT 非必填参数默认值 |
| `validateType` | number | IN/INOUT 必填 | `1=不校验`、`2=表达式校验`、`3=正则校验`；OUT 参数可不传 |
| `expression` | string | 按校验类型 | 正则或表达式 |
| `error` | string | 按校验类型 | 校验失败提示 |
| `description` | string | 否 | 参数说明 |

`orderNo` 的作用：

- JDBC 调用存储过程时按位置绑定参数，不按参数名绑定。
- `orderNo=1` 对应第一个 `?`，`orderNo=2` 对应第二个 `?`。
- 前端展示参数列表时建议按 `orderNo` 升序排列。

### 3.4 保存请求示例

```json
{
  "name": "产品列表存储过程",
  "code": "procFundList",
  "type": 2,
  "description": "通过存储过程查询产品列表",
  "orderNo": 10,
  "connectionId": 12,
  "procedureName": "PKG_FUND.QUERY_LIST",
  "fieldBackMode": 1,
  "apiParamList": [
    {
      "name": "产品代码",
      "code": "fundCode",
      "direction": 1,
      "jdbcType": "VARCHAR",
      "orderNo": 1,
      "required": 0,
      "defaultValue": "",
      "validateType": 1,
      "description": "按产品代码过滤"
    },
    {
      "name": "状态码",
      "code": "status",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 2,
      "description": "0表示成功"
    },
    {
      "name": "提示信息",
      "code": "message",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 3
    },
    {
      "name": "结果游标",
      "code": "data",
      "direction": 2,
      "jdbcType": "CURSOR",
      "orderNo": 4
    }
  ]
}
```

### 3.5 保存成功响应

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": {
    "id": 1001,
    "name": "产品列表存储过程",
    "code": "procFundList",
    "type": 2,
    "connectionId": 12,
    "procedureName": "PKG_FUND.QUERY_LIST",
    "apiParamList": []
  }
}
```

## 4. 查询与预览接口

### 4.1 查询详情

请求：

```json
{
  "id": 1001
}
```

或：

```json
{
  "code": "procFundList"
}
```

返回的 `data` 中包含存储过程主配置字段和 `apiParamList`。

### 4.2 分页查询

请求示例：

```json
{
  "type": 2,
  "name": "产品",
  "code": "proc",
  "pageNum": 1,
  "pageSize": 20
}
```

前端可用 `type=2` 筛选存储过程接口。

### 4.3 预览接口

请求：

```json
{
  "type": 2,
  "procedureName": "PKG_FUND.QUERY_LIST",
  "apiParamList": [
    {"code": "fundCode", "direction": 1, "jdbcType": "VARCHAR", "orderNo": 1},
    {"code": "status", "direction": 2, "jdbcType": "VARCHAR", "orderNo": 2},
    {"code": "message", "direction": 2, "jdbcType": "VARCHAR", "orderNo": 3},
    {"code": "data", "direction": 2, "jdbcType": "CURSOR", "orderNo": 4}
  ]
}
```

成功响应：

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": "{ call PKG_FUND.QUERY_LIST(? /* fundCode:IN:VARCHAR */, ? /* status:OUT:VARCHAR */, ? /* message:OUT:VARCHAR */, ? /* data:OUT:CURSOR */) }"
}
```

## 5. 执行存储过程

### 5.1 请求信息

| 项 | 值 |
| --- | --- |
| URL | `/api/actuator/execute` |
| Method | `POST` |
| Content-Type | `application/json` |

### 5.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tenant` | string | 按后端配置 | 租户标识 |
| `apiCode` | string | 是 | 接口编码，对应配置中的 `code` |
| `token` | string | 是 | 授权 token |
| `systemCode` | string | 是 | 调用方系统编码 |
| `params` | object | 按 IN/INOUT 配置 | 只传 IN、INOUT 参数；不要传 OUT 参数 |
| `exportExcel` | boolean | 否 | `true` 表示导出 Excel |

说明：

- 执行请求不传 `procedureName`。
- OUT 参数和 OUT 游标由后端从存储过程读取并返回。
- 存储过程执行不使用 SQL 查询接口的 `pageNeed/pageNum/pageSize` 分页逻辑。

### 5.3 普通执行请求示例

```json
{
  "tenant": "demo",
  "apiCode": "procFundList",
  "token": "调用token",
  "systemCode": "front-system",
  "params": {
    "fundCode": "FUND001"
  }
}
```

### 5.4 成功响应：OUT 参数 + 单游标

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": {
    "outParams": {
      "status": "0",
      "message": "success"
    },
    "cursors": {
      "data": [
        {
          "fund_code": "FUND001",
          "fund_name": "示例产品"
        }
      ]
    },
    "resultCount": 1
  }
}
```

### 5.5 成功响应：仅 OUT 参数

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": {
    "outParams": {
      "status": "0",
      "message": "处理成功",
      "batchNo": "B202607220001"
    },
    "cursors": {},
    "resultCount": 0
  }
}
```

### 5.6 失败响应

```json
{
  "code": 1002,
  "message": "操作失败：请检查接口权限！",
  "data": null
}
```

常见失败原因：

| 场景 | 说明 |
| --- | --- |
| `apiCode` 为空 | 接口编码必填 |
| token 无权限 | token 不存在、IP 不在白名单、授权分组未关联接口 |
| `systemCode` 为空 | 调用方系统编码必填 |
| 传入未知参数 | `params` 中存在未配置的 IN/INOUT 参数 |
| 必填参数缺失 | 配置为 `required=1` 的 IN/INOUT 参数未传 |
| 参数类型错误 | 入参不符合 `jdbcType` 要求 |
| 过程执行异常 | 数据源、过程名、参数顺序或过程内部执行异常 |

## 6. Excel 导出

### 6.1 导出请求

```json
{
  "tenant": "demo",
  "apiCode": "procFundList",
  "token": "调用token",
  "systemCode": "front-system",
  "exportExcel": true,
  "params": {
    "fundCode": "FUND001"
  }
}
```

### 6.2 导出规则

| 存储过程返回形态 | 导出规则 |
| --- | --- |
| 一个 OUT 游标 | 导出该游标的数据 |
| 无 OUT 游标 | 导出 `outParams` 键值表，列为 `name`、`value` |
| 多个 OUT 游标 | 暂不支持导出，后端返回错误 |

成功时直接返回 `.xlsx` 文件流。

| 响应头 | 值 |
| --- | --- |
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `Content-Disposition` | `attachment;filename=apiCode_yyyyMMddHHmmss.xlsx` |
| `Access-Control-Expose-Headers` | `Content-Disposition` |

## 7. 前端页面建议

### 7.1 配置页表单

当 `type=2` 时：

- 展示 `procedureName`。
- 隐藏或禁用 SQL 查询专用字段：`selectParam`、`fromParam`、`whereParamFixed`、`whereParamChange`、`groupParam`、`orderParam`、`page`。
- 参数表格展示：`name`、`code`、`direction`、`jdbcType`、`orderNo`、`required`、`defaultValue`、`validateType`、`expression`、`error`、`description`。
- OUT 参数不要求填写 `required`、`defaultValue`、`validateType`、`expression`、`error`。
- `jdbcType=CURSOR` 时，`direction` 固定为 OUT。
- 参数列表建议按 `orderNo` 升序展示。

### 7.2 执行/联调页

- 只展示 IN、INOUT 参数输入框。
- 不展示 OUT 参数输入框。
- 返回结果分两块展示：
  - `outParams`：键值信息，适合展示状态码、消息、批次号等。
  - `cursors`：每个游标一个表格，表格列由返回字段动态生成。
- 如果 `cursors` 为空，显示“无结果集，仅返回输出参数”。

## 8. 联调检查清单

1. 确认数据库已执行新增字段 DDL。
2. 确认接口配置 `type=2`。
3. 确认 `procedureName` 与数据库实际过程名一致。
4. 确认 `apiParamList.orderNo` 与存储过程签名顺序完全一致。
5. 确认 OUT 游标参数配置为 `direction=2`、`jdbcType=CURSOR`。
6. 确认执行请求只传 IN、INOUT 参数。
7. 确认 token 已授权当前接口，且 IP 白名单允许当前调用方。

