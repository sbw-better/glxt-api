# SQL 执行结果导出接口说明

适用项目：glxt-api

文档版本：v1.2

更新日期：2026-07-09

## 1. 接口概览

| 接口 | 方法 | 说明 | 响应 |
| --- | --- | --- | --- |
| `/api/actuator/execute/export` | POST | 执行 SQL，并生成临时 Excel 文件 | JSON，返回文件名 |
| `/api/common/download` | POST | 根据文件名下载临时文件 | 文件流 |

说明：

- `/api/actuator/execute/export` 不直接返回文件流。
- 导出接口成功后返回临时文件名。
- 文件下载由 `/api/common/download` 完成。
- 原 `/api/actuator/execute` 查询接口保持不变。

## 2. 生成导出文件

### 2.1 基本信息

| 项 | 值 |
| --- | --- |
| URL | `/api/actuator/execute/export` |
| Method | POST |
| Content-Type | `application/json` |
| 返回类型 | `ResultModel<String>` |
| 成功返回数据 | 临时 Excel 文件名 |

### 2.2 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tenant` | string | 按接口配置 | 租户标识。后端通过 `tenant + apiCode` 查询接口配置。 |
| `apiCode` | string | 是 | 接口代码。 |
| `token` | string | 是 | 接口调用 token，用于权限校验。 |
| `systemCode` | string | 是 | 调用方系统代码。 |
| `fieldAuth` | boolean | 否 | 字段鉴权开关，一般可不传。 |
| `pageNeed` | boolean | 否 | 是否分页。`true` 表示分页，`false` 表示不分页，不传时以后端接口配置为准。 |
| `pageNum` | integer | 分页时必填 | 当前页码，从 1 开始。 |
| `pageSize` | integer | 分页时必填 | 每页条数，最大不能超过 `common.sql_result_page_max_row`，当前为 5000。 |
| `params` | object | 按接口配置 | SQL 动态入参。不同接口参数不同。 |

### 2.3 params 参数说明

`params` 中的字段由接口配置决定。后端会校验参数是否存在、必填参数是否完整、参数类型是否正确。

特殊鉴权字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `managerField` | integer | 投顾或经理维度鉴权字段。 |
| `fundIdsField` | string | 产品 ID 列表鉴权字段。 |
| `fundCodesField` | string | 产品代码列表鉴权字段。 |

### 2.4 请求示例

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "fieldAuth": false,
  "pageNeed": true,
  "pageNum": 1,
  "pageSize": 20,
  "params": {
    "status": "1"
  }
}
```

### 2.5 成功响应

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260709153000.xlsx"
}
```

响应字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | integer | `0` 表示成功。 |
| `message` | string | 响应消息。 |
| `data` | string | 临时 Excel 文件名，用于调用下载接口。 |

### 2.6 失败响应

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
| `pageSize` 超限 | 当前最大 5000。 |
| 参数缺失或类型错误 | `params` 不符合接口参数配置。 |
| SQL 执行异常 | SQL 执行失败或被安全规则拦截。 |

## 3. 下载导出文件

### 3.1 基本信息

| 项 | 值 |
| --- | --- |
| URL | `/api/common/download` |
| Method | POST |
| Content-Type | `application/json` |
| 返回类型 | 文件流 |

### 3.2 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `fileName` | string | 是 | 无 | 导出接口返回的 `data`。 |
| `delete` | boolean | 否 | true | 下载后是否删除临时文件。 |
| `timeStamp` | boolean | 否 | true | 下载文件名是否追加当前时间戳。 |

### 3.3 请求示例

```json
{
  "fileName": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260709153000.xlsx",
  "delete": true,
  "timeStamp": false
}
```

### 3.4 响应说明

成功时返回文件流，响应头包含：

| 响应头 | 说明 |
| --- | --- |
| `Content-Type` | `application/octet-stream` |
| `Content-Disposition` | 下载文件名 |
| `Access-Control-Expose-Headers` | 暴露 `Content-Disposition` |

失败时返回：

```json
{
  "code": 1002,
  "message": "下载失败！",
  "data": null
}
```

## 4. 导出规则

| 规则 | 说明 |
| --- | --- |
| 导出范围 | 分页查询导出当前页数据；非分页查询导出本次 SQL 返回的数据。 |
| Sheet 名称 | 固定为 `result`。 |
| 表头 | 使用 SQL 返回字段名。 |
| 动态字段 | 不同 SQL 的字段可以不同，Excel 表头动态生成。 |
| 空结果 | 生成空 Excel 文件，不报错。 |
| null 值 | 导出为空单元格。 |
| 文件名 | `UUID_apiCode_yyyyMMddHHmmss.xlsx`。 |

## 5. 分页规则

| 场景 | 规则 |
| --- | --- |
| `pageNeed=true` | 分页，必须传 `pageNum` 和 `pageSize`。 |
| `pageNeed=false` | 不分页。 |
| `pageNeed` 不传 | 以后端接口配置 `API_SQL_INTERFACE.PAGE` 为准。 |
| 接口配置分页 | 必须传 `pageNum` 和 `pageSize`。 |
| 接口配置不分页 | 可以不传分页参数。 |

说明：`API_SQL_INTERFACE.PAGE` 是后端接口配置，不是前端入参。
