# double_value 存储过程接口测试说明

## 1. Oracle 存储过程

测试过程：

```sql
CREATE OR REPLACE PROCEDURE double_value (
  p_num IN OUT NUMBER
)
AS
BEGIN
  p_num := p_num * 2;
END;
/
```

参数说明：

| Oracle参数 | 方向 | Oracle类型 | 接口参数code | JDBC类型 |
|---|---|---|---|---|
| `p_num` | `IN OUT` | `NUMBER` | `pNum` | `DECIMAL` |

说明：接口参数 `code` 不需要和 Oracle 参数名完全一致，因为当前后端使用 `CallableStatement` 按位置绑定参数。这里使用 `pNum` 是为了符合 JSON 字段命名习惯。

## 2. 测试文件

已准备两个文件：

- [double-value-procedure-test.sql](D:/projects/glxt-api/docs/double-value-procedure-test.sql)：Oracle 建过程和数据库内手工验证脚本。
- [double-value-api-test.http](D:/projects/glxt-api/docs/double-value-api-test.http)：REST Client / IntelliJ HTTP Client 接口测试脚本。

## 3. 执行前替换变量

打开 [double-value-api-test.http](D:/projects/glxt-api/docs/double-value-api-test.http)，替换顶部变量：

```http
@baseUrl = http://10.23.114.39:30480
@tenant = glxt
@token = 替换为接口所在分组token
@systemCode = glxt-test
@connectionId = 替换为Oracle数据源ID
@apiCode = doubleValueProc
@apiId = 替换为保存接口返回的data.id
```

关键变量：

| 变量 | 说明 |
|---|---|
| `baseUrl` | 后端服务地址 |
| `tenant` | 租户编码 |
| `token` | 接口分组授权 token |
| `connectionId` | Oracle 数据源在 `API_SQL_CONNECTION` 中的 ID |
| `apiCode` | 本次测试接口编码 |
| `apiId` | 保存接口配置后返回的接口 ID |

## 4. 接口配置保存

请求：

```http
POST /api/interface/save
```

核心配置：

```json
{
  "type": 2,
  "procedureName": "double_value",
  "connectionId": 1001,
  "apiParamList": [
    {
      "code": "pNum",
      "direction": 3,
      "jdbcType": "DECIMAL",
      "orderNo": 1,
      "required": 1,
      "validateType": 1
    }
  ]
}
```

字段含义：

| 字段 | 值 | 说明 |
|---|---:|---|
| `type` | `2` | 存储过程接口 |
| `procedureName` | `double_value` | Oracle 过程名 |
| `direction` | `3` | `INOUT` 参数 |
| `jdbcType` | `DECIMAL` | 对应 Oracle `NUMBER` |
| `orderNo` | `1` | 第 1 个过程参数 |
| `required` | `1` | 入参必填 |
| `validateType` | `1` | 不做正则/表达式校验 |

预期：保存成功，响应 `data.id` 是接口 ID。后续脚本中的 `@apiId` 替换为该值。

## 5. 调用预览

请求：

```http
POST /api/interface/preview
```

预期返回：

```text
{ call double_value(? /* pNum:INOUT:DECIMAL */) }
```

用途：确认后端生成的 `CallableStatement` 调用语句和参数顺序正确。

## 6. 配置查询接口

覆盖接口：

| 接口 | 目的 |
|---|---|
| `POST /api/interface/get` | 按 ID 或 code 查询单个接口配置 |
| `POST /api/interface/list` | 列表查询 |
| `POST /api/interface/page` | 分页查询 |
| `POST /api/interface/demo` | 获取调用 demo |
| `POST /api/interface/have_page` | 查询接口是否分页 |
| `POST /api/interface/export` | 按 `ids` 导出接口配置 |

重点检查：

- 返回字段包含 `type=2`。
- 返回字段包含 `procedureName=double_value`。
- 参数列表包含 `direction=3`、`jdbcType=DECIMAL`、`orderNo=1`。
- `have_page` 预期返回 `false`。

## 7. 正常执行测试

请求：

```http
POST /api/actuator/execute
```

入参：

```json
{
  "params": {
    "pNum": 5
  }
}
```

预期业务结果：

```json
{
  "outParams": {
    "pNum": 10
  },
  "cursors": {},
  "resultCount": 0
}
```

说明：

- `pNum` 是 `INOUT` 参数，既作为入参传给 Oracle，又作为 OUT 标量返回。
- 该过程没有 `SYS_REFCURSOR`，所以 `cursors` 为空。
- 没有游标行数，所以 `resultCount=0`。

小数测试：

```json
{
  "params": {
    "pNum": 12.5
  }
}
```

预期 `outParams.pNum=25`。

## 8. 异常执行测试

脚本中包含以下异常场景：

| 场景 | 请求特点 | 预期 |
|---|---|---|
| 必填参数缺失 | `params={}` | 提示 `pNum` 不能为空 |
| 必填参数为 null | `"pNum": null` | 提示 `pNum` 不能为空 |
| 参数类型错误 | `"pNum": "abc"` | 提示应为数值类型 |
| 未知参数 | 多传 `unknown` | 提示配置中不存在 IN/INOUT 参数 |
| token 错误 | `token=wrong-token` | 权限校验失败，不执行过程 |

这些场景用于验证：

- 入参必填校验生效。
- `DECIMAL` 类型转换生效。
- OUT 参数限制和未知参数拦截生效。
- 授权失败不会进入存储过程执行。

## 9. Excel 导出测试

请求仍然使用：

```http
POST /api/actuator/execute
```

增加：

```json
{
  "exportExcel": true
}
```

由于 `double_value` 没有游标，预期导出 `outParams` 键值表：

| name | value |
|---|---|
| pNum | 10 |

## 10. 执行记录查询

请求：

```http
POST /api/actuator/page
```

重点检查执行记录：

| 字段 | 预期 |
|---|---|
| `apiCode` | `doubleValueProc` |
| `executeSql` | `{ call double_value(?) }` |
| `resultCount` | `0` |
| `executeResult` | 成功或失败状态 |
| `executeResultDetail` | 失败时记录错误信息 |

## 11. 接口配置导出

请求：

```http
POST /api/interface/export
```

该接口后端只读取 `ids` 字段，不按 `tenant/type/code` 过滤。保存接口配置后，需要把响应中的 `data.id` 填到 `@apiId`。

正确请求体：

```json
{
  "ids": [
    1001
  ]
}
```

## 12. 清理接口配置

脚本最后提供：

```http
POST /api/interface/delete
```

只在测试完成后执行。执行后会删除接口配置。
