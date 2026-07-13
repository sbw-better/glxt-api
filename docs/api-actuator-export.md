# SQL 执行结果导出接口说明

## 调用流程

1. 前端调用 `/api/actuator/execute`。
2. 请求体不传 `exportExcel` 或传 `false` 时，接口直接返回原 SQL 查询结果。
3. 请求体传 `exportExcel=true` 时，接口生成临时 Excel 文件，并返回临时文件名。
4. 前端拿返回的文件名调用 `/api/common/download` 下载文件。

导出不再使用单独的导出接口。

## 1. 执行 SQL 或生成导出文件

- URL: `/api/actuator/execute`
- Method: `POST`
- Content-Type: `application/json`
- Response: 统一 `ResultModel`

### 查询请求示例

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

### 导出请求示例

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "exportExcel": true,
  "fieldAuth": false,
  "pageNeed": true,
  "pageNum": 1,
  "pageSize": 20,
  "params": {
    "status": "1"
  }
}
```

### 导出成功响应

```json
{
  "code": 0,
  "message": "操作成功！",
  "data": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260710153000.xlsx"
}
```

`data` 是临时文件名，前端需要原样传给通用下载接口。

### 失败响应

```json
{
  "code": 1002,
  "message": "操作失败：no permission",
  "data": null
}
```

常见失败原因包括 token 无权限、IP 不在白名单、必填参数缺失、参数类型不合法、SQL 执行失败。

## 2. 下载导出文件

- URL: `/api/common/download`
- Method: `POST`
- Content-Type: `application/json`
- Response: Excel 文件流

### 请求示例

```json
{
  "fileName": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260710153000.xlsx",
  "delete": true,
  "timeStamp": false
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `fileName` | string | 是 | `/api/actuator/execute` 导出成功时返回的 `data` |
| `delete` | boolean | 否 | 是否下载后删除临时文件，建议传 `true` |
| `timeStamp` | boolean | 否 | 是否由下载接口再追加时间戳，建议传 `false` |

## 导出范围

- 如果 `pageNeed=true`，导出当前 `pageNum/pageSize` 对应的当前页数据。
- 如果 `pageNeed` 不传，并且接口配置为分页，也导出当前页数据。
- 如果接口不分页，导出本次 SQL 查询返回的全部结果。

## Excel 内容规则

- Sheet 名称固定为 `result`。
- 表头使用 SQL 查询结果字段名。
- 不同 SQL 字段不同，表头会随返回字段动态变化。
- 多行字段不完全一致时，表头取所有行字段的并集，按首次出现顺序排列。
- `null` 值导出为空单元格。
