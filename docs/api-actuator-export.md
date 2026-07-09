# SQL 执行结果导出接口说明

## 调用流程

1. 前端调用导出接口生成临时 Excel 文件。
2. 导出接口返回临时文件名。
3. 前端拿返回的文件名调用通用下载接口下载文件。

导出接口不会直接返回文件流。

## 1. 生成导出文件

- URL: `/api/actuator/execute/export`
- Method: `POST`
- Content-Type: `application/json`
- Response: 统一 `ResultModel`

请求体与 `/api/actuator/execute` 保持一致。

### 请求示例

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

### 成功响应

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260630153000.xlsx"
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
  "fileName": "0d4c7b54-7d59-4c17-a4af-76a68b3f2201_demoApi_20260630153000.xlsx",
  "delete": true,
  "timeStamp": false
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `fileName` | string | 是 | 第一步导出接口返回的 `data` |
| `delete` | boolean | 否 | 是否下载后删除临时文件，建议传 `true` |
| `timeStamp` | boolean | 否 | 是否由下载接口再追加时间戳，建议传 `false` |

## 前端处理建议

导出接口响应是 JSON，下载接口响应是文件流，两次请求的响应类型不同。

```js
async function exportSqlResult(payload) {
  const exportResp = await fetch('/api/actuator/execute/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(res => res.json());

  if (exportResp.code !== 0) {
    throw new Error(exportResp.message || '导出失败');
  }

  const downloadResp = await fetch('/api/common/download', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fileName: exportResp.data,
      delete: true,
      timeStamp: false
    })
  });

  const blob = await downloadResp.blob();
  const disposition = downloadResp.headers.get('Content-Disposition') || '';
  const fileName = decodeURIComponent((disposition.match(/filename=([^;]+)/) || [])[1] || exportResp.data);
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}
```

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
