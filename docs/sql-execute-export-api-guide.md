# SQL 执行与 Excel 导出接口说明

适用项目：glxt-api

文档版本：v3.0

更新日期：2026-07-21

## 1. 接口概览

| 接口 | 方法 | 说明 | 成功响应 |
| --- | --- | --- | --- |
| `/api/actuator/execute` | POST | 执行 SQL；通过 `exportExcel` 控制返回查询数据或 Excel 文件 | JSON 或 `.xlsx` 文件流 |

说明：

- 不再单独提供 SQL 结果导出接口。
- `exportExcel` 不传或为 `false` 时，保持原逻辑，返回 SQL 查询数据 JSON。
- `exportExcel=true` 时，同一个接口直接返回 Excel 文件流。
- SQL 结果导出成功时已经是文件流响应，不需要二次请求下载。

## 2. 请求信息

| 项 | 值 |
| --- | --- |
| URL | `/api/actuator/execute` |
| Method | POST |
| Content-Type | `application/json` |

## 3. 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tenant` | string | 按接口配置 | 租户标识。后端通过 `tenant + apiCode` 查询接口配置。 |
| `apiCode` | string | 是 | 接口代码。 |
| `token` | string | 是 | 接口调用 token，用于权限校验。 |
| `systemCode` | string | 是 | 调用方系统代码。 |
| `exportExcel` | boolean | 否 | 是否导出 Excel。`true` 表示直接返回 Excel 文件流；不传或 `false` 表示返回原查询数据。 |
| `fieldAuth` | boolean | 否 | 字段鉴权开关，一般可不传。 |
| `pageNeed` | boolean | 否 | 是否分页。`true` 表示分页，`false` 表示不分页，不传时以后端接口配置为准。 |
| `pageNum` | integer | 分页时必填 | 当前页码，从 1 开始。 |
| `pageSize` | integer | 分页时必填 | 每页条数，最大不能超过 `common.sql_result_page_max_row`。 |
| `params` | object | 按接口配置 | SQL 动态入参。不同接口参数不同。 |

## 4. 普通查询

### 请求示例

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "pageNeed": true,
  "pageNum": 1,
  "pageSize": 20,
  "params": {
    "status": "1"
  }
}
```

### 成功响应

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": {
    "pageNum": 1,
    "pageSize": 20,
    "total": 31,
    "list": []
  }
}
```

说明：查询响应数据结构由 SQL 是否分页以及查询结果决定，保持原逻辑不变。

## 5. Excel 导出

### 请求示例

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "exportExcel": true,
  "pageNeed": true,
  "pageNum": 1,
  "pageSize": 20,
  "params": {
    "status": "1"
  }
}
```

### 成功响应

成功时直接返回 `.xlsx` 文件流。

| 响应头 | 值 |
| --- | --- |
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `Content-Disposition` | `attachment;filename=apiCode_yyyyMMddHHmmss.xlsx` |
| `Access-Control-Expose-Headers` | `Content-Disposition` |

文件名示例：

```text
demoApi_20260721143000.xlsx
```

## 6. 失败响应

普通查询和 Excel 导出失败时，仍返回 JSON 错误结构。

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
| `apiCode` 为空 | 接口代码必填。 |
| token 无权限 | 当前 token 无该接口执行权限，或 IP 不在允许范围内。 |
| `systemCode` 为空 | 系统代码必填。 |
| 分页参数缺失 | 分页时必须传 `pageNum` 和 `pageSize`。 |
| 分页参数非法 | `pageNum` 和 `pageSize` 必须大于 0。 |
| `pageSize` 超限 | 不能超过系统配置的最大分页条数。 |
| 参数缺失或类型错误 | `params` 不符合接口参数配置。 |
| SQL 执行异常 | SQL 执行失败或被安全规则拦截。 |

## 7. 导出规则

| 规则 | 说明 |
| --- | --- |
| 导出入口 | `/api/actuator/execute`，请求体中传 `exportExcel=true`。 |
| 导出范围 | 分页查询导出当前页数据；非分页查询导出本次 SQL 返回的数据。 |
| Sheet 名称 | 固定为 `result`。 |
| 表头 | 使用 SQL 返回字段名。 |
| 动态字段 | 不同 SQL 的字段可以不同，Excel 表头动态生成。 |
| 空结果 | 生成空 Excel 文件，不报错。 |
| null 值 | 导出为空单元格。 |
| 文件名 | `apiCode_yyyyMMddHHmmss.xlsx`。 |

## 8. 分页规则

| 场景 | 规则 |
| --- | --- |
| `pageNeed=true` | 分页，必须传 `pageNum` 和 `pageSize`。 |
| `pageNeed=false` | 不分页。 |
| `pageNeed` 不传 | 以后端接口配置 `API_SQL_INTERFACE.PAGE` 为准。 |
| 接口配置分页 | 必须传 `pageNum` 和 `pageSize`。 |
| 接口配置不分页 | 可以不传分页参数。 |

说明：`API_SQL_INTERFACE.PAGE` 是后端接口配置，不是前端入参。
