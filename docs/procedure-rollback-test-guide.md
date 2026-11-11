# 存储过程事务回滚测试

此测试用专用表和三个过程验证 API 的事务边界：失败过程插入一行后抛出异常；校验过程读取行数；清理过程可删除指定测试 ID。过程里不执行 `COMMIT`，以便由 API 事务回滚插入。

## 1. 创建测试对象

使用 API 所配置的 Oracle schema 执行 [`procedure-rollback-test.sql`](procedure-rollback-test.sql)。脚本会删除并重建同名测试对象；不要在有同名业务对象的 schema 执行。

## 2. 导入 Postman 集合

导入 [`procedure-rollback-postman-collection.json`](procedure-rollback-postman-collection.json)，设置变量：

| 变量 | 示例/说明 |
| --- | --- |
| `baseUrl` | API 地址，例如 `http://localhost:15009` |
| `tenant` | 当前环境中的租户编码 |
| `token` | 该租户下有执行权限的 token |
| `systemCode` | 调用方系统编码 |
| `connectionId` | 使用上述测试 schema 的数据源 ID，数字，不带引号 |

先运行 **1. Save failing procedure config** 和 **2. Save verification procedure config**。集合会生成唯一 API code，后续请求自动引用。保存配置会在 GLXT API 配置库中新增两条测试接口。

## 3. 执行测试

按顺序运行：

1. **3. Execute failing procedure**：预期接口返回失败响应，其中包含 `ROLLBACK_PROBE_FORCED_FAILURE`。过程执行前已插入测试行。
2. **4. Verify row was rolled back**：Postman 断言 OUT 参数 `rowCount` 等于 `0`。该断言证明异常后插入没有保留。

Postman collection 的测试脚本从执行响应读取 `testId`，再将其传给校验过程。若测试断言失败，可在 Oracle 执行：

```sql
SELECT REQUEST_ID, CREATED_AT
  FROM CODEX_PROC_ROLLBACK_TEST
 WHERE REQUEST_ID = '<Postman 控制台中的 testId>';
```

## 4. 清理

成功回滚时无需清理行。可选地执行 `PROC_ROLLBACK_PROBE_CLEAN` 清理指定 ID；确认不再测试后，执行 SQL 文件末尾的 teardown 语句，删除三个过程和专用表。另请在接口管理页面删除两条测试接口配置。

如果存储过程内部使用 `COMMIT` 或自治事务，本测试预期会检测到行仍存在；外层 Spring 事务无法回滚这些内部提交。
