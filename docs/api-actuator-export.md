# SQL 执行结果导出接口说明

## 调用入口

- URL: `/api/actuator/execute`
- Method: `POST`
- Content-Type: `application/json`

导出不再使用单独接口；成功响应已经是文件流，不需要二次请求下载。

## 调用规则

1. 请求体不传 `exportExcel` 或传 `false` 时，接口返回原 SQL 查询结果 JSON。
2. 请求体传 `exportExcel=true` 时，接口直接返回 `.xlsx` 文件流。
3. 查询和导出共用同一套 SQL 校验、权限校验、分页、数据源切换和审计逻辑。

## 查询请求示例

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

## 导出请求示例

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

## 导出成功响应

成功时直接返回 Excel 文件流，响应头如下：

| 响应头 | 值 |
| --- | --- |
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `Content-Disposition` | `attachment;filename=apiCode_yyyyMMddHHmmss.xlsx` |
| `Access-Control-Expose-Headers` | `Content-Disposition` |

## 失败响应

普通查询和导出失败时都返回 JSON。

```json
{
  "code": 1002,
  "message": "操作失败：no permission",
  "data": null
}
```

常见失败原因包括 token 无权限、IP 不在白名单、必填参数缺失、参数类型不合法、SQL 执行失败。

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
