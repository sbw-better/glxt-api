# employees_test 存储过程测试设计与接口脚本说明

## 1. 文件清单

本次新增两个测试文件：

- [employees-procedure-test.sql](D:/projects/glxt-api/docs/employees-procedure-test.sql)：Oracle 11g 建表、补数据、创建测试存储过程。
- [employees-procedure-api-test.http](D:/projects/glxt-api/docs/employees-procedure-api-test.http)：glxt-api 接口配置和执行测试脚本。

## 2. 测试表

表结构：

```sql
CREATE TABLE employees_test (
  emp_id      NUMBER PRIMARY KEY,
  emp_name    VARCHAR2(50),
  salary      NUMBER,
  dept_id     NUMBER,
  create_time DATE DEFAULT SYSDATE
);
```

基础数据和补充数据：

| emp_id | emp_name | salary | dept_id | 用途 |
|---:|---|---:|---:|---|
| 1 | 张三 | 5000 | 10 | 单条查询命中 |
| 2 | 李四 | 7000 | 10 | 部门 + 最低薪资过滤 |
| 3 | 王五 | 9000 | 20 | 高薪、涨薪 |
| 4 | 赵六 | 3000 | 20 | 低薪、涨薪 |
| 5 | 钱七 | 12000 | 30 | 高薪排行 |
| 6 | 孙八 | 7000 | 30 | 边界薪资 |

## 3. 存储过程覆盖范围

| 存储过程 | 覆盖能力 | 对应接口code |
|---|---|---|
| `proc_emp_get` | `IN` + 多个 `OUT` 标量、无数据返回 | `procEmpGet` |
| `proc_salary_double` | `INOUT NUMBER` | `procSalaryDouble` |
| `proc_emp_query` | 可选 `IN` 条件 + `OUT SYS_REFCURSOR` + `OUT` 状态 | `procEmpQuery` |
| `proc_emp_insert` | DML 插入 + 成功/主键重复异常返回 + 无游标导出 | `procEmpInsert` |
| `proc_emp_raise_salary` | DML 更新 + OUT 影响行数 | `procEmpRaiseSalary` |
| `proc_emp_multi_cursor` | 多个 `OUT SYS_REFCURSOR` + 多游标 Excel 限制 | `procEmpMultiCursor` |

## 4. 执行顺序

1. 在 Oracle 执行 [employees-procedure-test.sql](D:/projects/glxt-api/docs/employees-procedure-test.sql)。
2. 打开 [employees-procedure-api-test.http](D:/projects/glxt-api/docs/employees-procedure-api-test.http)。
3. 替换顶部变量：

```http
@baseUrl = http://10.23.114.39:30480
@tenant = glxt
@token = 替换为接口所在分组token
@systemCode = glxt-proc-test
@connectionId = 替换为Oracle数据源ID
```

4. 依次执行每个 `save` 请求。
5. 将保存响应中的 `data.id` 填到对应变量：

```http
@apiEmpGetId = ...
@apiSalaryDoubleId = ...
@apiEmpQueryId = ...
@apiEmpInsertId = ...
@apiEmpRaiseSalaryId = ...
@apiEmpMultiCursorId = ...
```

6. 执行后续预览、执行、导出、记录查询请求。
7. 接口配置导出使用 `/api/interface/export`，请求体必须传 `ids`，不能用 `tenant/type/code` 作为导出筛选条件。

## 5. 参数配置规则

### 5.1 NUMBER 参数

Oracle `NUMBER` 推荐配置为：

```json
{
  "jdbcType": "DECIMAL",
  "type": 3
}
```

说明：

- `jdbcType=DECIMAL` 用于 `CallableStatement` 注册和绑定。
- `type=3` 用于前端 demo 展示和数字语义表达。

### 5.2 VARCHAR2 参数

Oracle `VARCHAR2` 配置为：

```json
{
  "jdbcType": "VARCHAR",
  "type": 1
}
```

### 5.3 SYS_REFCURSOR 参数

Oracle `SYS_REFCURSOR` 配置为：

```json
{
  "direction": 2,
  "jdbcType": "CURSOR"
}
```

限制：

- 当前后端一期只支持 `OUT CURSOR`。
- 不支持 `IN CURSOR` 或 `INOUT CURSOR`。

### 5.4 参数顺序

所有存储过程参数都必须配置 `orderNo`。

`orderNo` 必须与 Oracle 过程定义顺序完全一致。当前后端使用 `CallableStatement` 按位置绑定，不按参数名绑定。

## 6. 重点测试用例

### 6.1 IN + OUT 标量

接口：`procEmpGet`

请求：

```json
{
  "params": {
    "empId": 1
  }
}
```

预期：

```json
{
  "outParams": {
    "empName": "张三",
    "salary": 5000,
    "deptId": 10,
    "status": "0",
    "message": "success"
  },
  "cursors": {},
  "resultCount": 0
}
```

无数据请求：

```json
{
  "params": {
    "empId": 999
  }
}
```

预期：

```json
{
  "outParams": {
    "status": "404",
    "message": "employee not found"
  }
}
```

## 6.2 INOUT 参数

接口：`procSalaryDouble`

请求：

```json
{
  "params": {
    "salary": 5000
  }
}
```

预期：

```json
{
  "outParams": {
    "salary": 10000
  },
  "cursors": {},
  "resultCount": 0
}
```

## 6.3 单游标查询

接口：`procEmpQuery`

按部门查询：

```json
{
  "params": {
    "deptId": 10
  }
}
```

预期：

- `outParams.status=0`
- `cursors.data` 返回张三、李四
- `resultCount=2`

部门 + 最低薪资：

```json
{
  "params": {
    "deptId": 10,
    "minSalary": 6000
  }
}
```

预期：

- `cursors.data` 只返回李四
- `resultCount=1`

可选参数显式传 null：

```json
{
  "params": {
    "deptId": null,
    "minSalary": null
  }
}
```

预期：

- 后端使用 `setNull(index, Types.DECIMAL)` 绑定 Oracle 空值。
- 返回所有员工。

## 6.4 DML 插入

接口：`procEmpInsert`

新增成功：

```json
{
  "params": {
    "empId": 100,
    "empName": "测试员工",
    "salary": 8800,
    "deptId": 40
  }
}
```

预期：

```json
{
  "outParams": {
    "status": "0",
    "message": "insert success"
  }
}
```

主键重复：

```json
{
  "params": {
    "empId": 1,
    "empName": "重复员工",
    "salary": 6600,
    "deptId": 10
  }
}
```

预期：

```json
{
  "outParams": {
    "status": "409",
    "message": "employee id already exists"
  }
}
```

## 6.5 DML 更新

接口：`procEmpRaiseSalary`

请求：

```json
{
  "params": {
    "deptId": 20,
    "raiseRate": 0.1
  }
}
```

预期：

- `outParams.rows=2`
- `outParams.status=0`
- 部门 20 的王五、赵六薪资增长 10%

注意：该过程内部执行 `COMMIT`，符合当前设计“不由服务端包事务”。

## 6.6 多游标

接口：`procEmpMultiCursor`

普通执行：

```json
{
  "params": {}
}
```

预期：

```json
{
  "outParams": {},
  "cursors": {
    "deptData": [],
    "topData": []
  },
  "resultCount": 0
}
```

实际 `resultCount` 为两个游标行数合计。

Excel 导出：

```json
{
  "exportExcel": true,
  "params": {}
}
```

预期失败：

```text
存储过程多游标结果暂不支持Excel导出
```

## 7. 异常场景

| 场景 | 脚本位置 | 预期 |
|---|---|---|
| 必填参数缺失 | `proc_emp_get` 参数缺失 | 提示必传参数不能为空 |
| 参数类型错误 | 可把 `empId` 改为 `"abc"` | 提示应为数值类型 |
| 未知参数 | 可额外传 `unknown` | 提示配置中不存在 IN/INOUT 参数 |
| token 错误 | 将 `token` 改为错误值 | 权限校验失败 |
| 单游标 Excel | `procEmpQuery` 导出 | 成功下载游标数据 |
| 无游标 Excel | `procEmpInsert` 导出 | 成功下载 outParams 键值表 |
| 多游标 Excel | `procEmpMultiCursor` 导出 | 返回不支持错误 |

## 8. 接口配置导出

接口：

```http
POST /api/interface/export
```

该接口后端只读取 `ids` 字段。要导出本测试中的 6 个存储过程接口，先把每个保存响应里的 `data.id` 填到 `.http` 文件顶部变量，再执行导出请求。

正确请求体：

```json
{
  "ids": [
    1001,
    1002,
    1003,
    1004,
    1005,
    1006
  ]
}
```

错误写法：

```json
{
  "tenant": "glxt",
  "type": 2,
  "code": "procEmpQuery"
}
```

上述写法不会被当前导出逻辑使用。

## 9. 验收点

接口联调时重点确认：

- `preview` 返回 `{ call proc_name(?, ...) }`。
- `INOUT` 参数进入 `outParams`。
- `OUT SYS_REFCURSOR` 参数进入 `cursors`。
- `resultCount` 等于所有游标行数合计。
- 执行记录 `executeSql` 保存 `{ call ... }`。
- 执行失败时也能写入执行记录。
- Excel 单游标、无游标、多游标行为符合设计。
