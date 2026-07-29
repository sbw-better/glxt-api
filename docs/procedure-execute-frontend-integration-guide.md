# 存储过程接口前端调用与执行逻辑说明

## 1. 文档目的

本文面向前端开发，说明本次“存储过程配置与调用执行”需求对接口配置、调用、导出、返回结构产生的影响。

重点说明：

- 新增字段和枚举值。
- 前端保存配置时需要传什么。
- `/api/actuator/execute` 如何根据接口类型分流。
- 存储过程入参、出参、游标结果如何处理。
- Excel 导出有哪些限制。
- 哪些逻辑是本次新增，哪些保持原 SQL 接口兼容。

## 2. 核心修改点总览

### 2.1 接口类型新增

原来接口类型只有 SQL 查询，本次新增存储过程类型：

| 字段 | 旧逻辑 | 新逻辑 |
|---|---|---|
| `type` | `1=SQL查询` | 新增 `2=存储过程` |

前端判断规则：

```text
type == 1 或 type 为空：按原 SQL 查询接口处理
type == 2：按存储过程接口处理
```

注意：后端为了兼容旧数据，`type` 为空时仍按 SQL 查询处理。

### 2.2 接口主表新增字段

表：`API_SQL_INTERFACE`

| 字段 | 前端字段 | 说明 |
|---|---|---|
| `PROCEDURE_NAME` | `procedureName` | 存储过程完整名称 |

支持格式：

```text
PROC_NAME
PKG_NAME.PROC_NAME
SCHEMA.PKG_NAME.PROC_NAME
```

当前后端允许字符：

```text
字母、数字、下划线、点、$、#
```

对应校验：

```text
[A-Za-z0-9_.$#]+
```

### 2.3 参数表新增字段

表：`API_SQL_INTERFACE_PARAM`

| 字段 | 前端字段 | 说明 |
|---|---|---|
| `DIRECTION` | `direction` | 存储过程参数方向 |
| `JDBC_TYPE` | `jdbcType` | JDBC 参数类型 |
| `ORDER_NO` | `orderNo` | 过程参数绑定顺序 |

## 3. 参数枚举说明

### 3.1 参数方向 `direction`

| 展示值 | 提交值 | 含义 | 前端是否传入 |
|---|---:|---|---|
| `IN` | `1` | 输入参数 | 是 |
| `OUT` | `2` | 输出参数 | 否 |
| `INOUT` | `3` | 输入输出参数 | 是 |

前端规则：

- `IN`：调用时必须或可选传入，取决于 `required`。
- `OUT`：调用时不能传入，由数据库返回。
- `INOUT`：调用时传入，执行后也会在 `outParams` 中返回。

### 3.2 JDBC 类型 `jdbcType`

| 提交值 | Oracle 常见类型 | 说明 |
|---|---|---|
| `VARCHAR` | `VARCHAR2`、`CHAR` | 字符串 |
| `INTEGER` | `NUMBER(10)` | 整型 |
| `BIGINT` | `NUMBER` 大整数 | 长整型 |
| `DECIMAL` | `NUMBER(p,s)` | 数值/小数，Oracle `NUMBER` 推荐优先用这个 |
| `DATE` | `DATE` | 日期 |
| `TIMESTAMP` | `TIMESTAMP` | 时间戳 |
| `CURSOR` | `SYS_REFCURSOR` | Oracle 游标结果集 |

注意：

- `CURSOR` 当前只支持 `OUT`。
- `IN CURSOR`、`INOUT CURSOR` 当前不支持。
- Oracle `NUMBER` 如果不确定精度，前端配置建议使用 `DECIMAL`。

### 3.3 参数顺序 `orderNo`

`orderNo` 是存储过程参数绑定位置。

后端使用：

```java
CallableStatement
```

按位置绑定参数，不按参数名绑定。因此 `orderNo` 必须和数据库过程定义顺序完全一致。

示例：

```sql
PROCEDURE proc_emp_get (
  p_emp_id   IN  NUMBER,
  p_emp_name OUT VARCHAR2,
  p_salary   OUT NUMBER
)
```

前端参数配置必须是：

| orderNo | code | direction | jdbcType |
|---:|---|---:|---|
| 1 | `empId` | `1` | `DECIMAL` |
| 2 | `empName` | `2` | `VARCHAR` |
| 3 | `salary` | `2` | `DECIMAL` |

后端最终调用：

```sql
{ call proc_emp_get(?, ?, ?) }
```

如果 `orderNo` 配错，会导致 Oracle 参数绑定错位。

## 4. 页面配置逻辑

### 4.1 SQL 查询接口

当 `type=1` 或 `type` 为空时：

- 页面保持原 SQL 配置逻辑。
- `selectParam/fromParam/whereParamFixed/whereParamChange/groupParam/orderParam/page` 继续展示和校验。
- 参数表继续使用原字段：`type/required/defaultValue/validateType/expression/error`。
- 执行结果、分页、Excel 导出保持原逻辑。

### 4.2 存储过程接口

当 `type=2` 时：

前端应展示：

- `name`
- `code`
- `type`
- `description`
- `orderNo`
- `connectionId`
- `fieldBackMode`
- `procedureName`
- `apiParamList`

前端应隐藏或禁用 SQL 查询字段：

- `selectParam`
- `fromParam`
- `whereParamFixed`
- `whereParamChange`
- `groupParam`
- `orderParam`
- `managerField`
- `fundIdsField`
- `fundCodesField`

建议：

- `page` 固定传 `0`。
- 存储过程当前不使用 SQL 分页逻辑。

## 5. 保存配置接口

### 5.1 接口地址

```http
POST /api/interface/save
```

### 5.2 存储过程保存请求示例

```json
{
  "tenant": "glxt",
  "name": "员工列表游标查询",
  "code": "procEmpQuery",
  "type": 2,
  "description": "测试OUT SYS_REFCURSOR和可选过滤条件",
  "orderNo": 103,
  "connectionId": 1001,
  "fieldBackMode": 1,
  "page": 0,
  "procedureName": "proc_emp_query",
  "apiParamList": [
    {
      "name": "部门ID",
      "code": "deptId",
      "type": 3,
      "direction": 1,
      "jdbcType": "DECIMAL",
      "orderNo": 1,
      "required": 0,
      "defaultValue": null,
      "validateType": 1
    },
    {
      "name": "状态码",
      "code": "status",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 2
    },
    {
      "name": "员工列表",
      "code": "data",
      "direction": 2,
      "jdbcType": "CURSOR",
      "orderNo": 3
    }
  ]
}
```

### 5.3 保存校验变化

本次新增的后端校验：

| 校验点 | 说明 |
|---|---|
| `type` | 只能是 `1` 或 `2` |
| `procedureName` | 存储过程类型必填 |
| `connectionId` | 存储过程类型必填 |
| `direction` | 每个过程参数必填 |
| `jdbcType` | 每个过程参数必填 |
| `orderNo` | 每个过程参数必填 |
| `orderNo` 唯一 | 同一个接口内不能重复 |
| `CURSOR` 方向 | 只允许 `OUT` |
| `IN/INOUT` 参数 | 必须配置 `required` 和 `validateType` |
| `OUT` 参数 | 不要求 `required/defaultValue/validateType/expression/error` |

前端建议在保存前同步做这些校验，避免无效请求进后端。

## 6. 调用预览接口

### 6.1 接口地址

```http
POST /api/interface/preview
```

### 6.2 请求示例

```json
{
  "type": 2,
  "procedureName": "proc_emp_get",
  "apiParamList": [
    {
      "code": "empId",
      "direction": 1,
      "jdbcType": "DECIMAL",
      "orderNo": 1
    },
    {
      "code": "empName",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 2
    }
  ]
}
```

### 6.3 返回示例

```text
{ call proc_emp_get(? /* empId:IN:DECIMAL */, ? /* empName:OUT:VARCHAR */) }
```

前端用途：

- 展示调用语句。
- 辅助确认 `orderNo` 是否正确。
- 辅助确认 `IN/OUT/INOUT` 和 `jdbcType` 是否配置正确。

## 7. 获取调用 Demo

### 7.1 接口地址

```http
POST /api/interface/demo
```

### 7.2 请求示例

```json
{
  "code": "procEmpQuery"
}
```

### 7.3 存储过程 demo 变化

存储过程类型下：

- demo 中会带上 `procedureName`。
- `params` 只展示 `IN` 和 `INOUT` 参数。
- `OUT` 参数不会展示，因为调用方不能传。

示例：

```json
{
  "token": "替换为接口所在分组的token值",
  "systemCode": "替换为接口调用方的系统代码，如\"glxt\"",
  "tenant": "glxt",
  "apiCode": "procEmpQuery",
  "procedureName": "proc_emp_query",
  "params": {
    "deptId(非必传，不传取默认值，传则删掉括号内容)": 1.0
  }
}
```

## 8. 执行接口

### 8.1 接口地址

普通执行和 Excel 导出都复用：

```http
POST /api/actuator/execute
```

本次不新增存储过程专用执行接口。

### 8.2 后端执行分流

后端逻辑：

```text
根据 tenant + apiCode 查询接口配置
  |
  |-- type 为空或 type=1：走原 SQL 查询执行逻辑
  |
  |-- type=2：走存储过程执行逻辑
```

对应代码入口：

```text
ApiActuatorServiceImpl.execute
```

注意：

- 前端执行时不需要传 `type`。
- 后端根据接口配置表中的 `type` 判断执行分支。
- 旧 SQL 接口请求体不变。

### 8.3 执行请求示例

```json
{
  "tenant": "glxt",
  "apiCode": "procEmpQuery",
  "token": "接口分组token",
  "systemCode": "glxt-test",
  "params": {
    "deptId": 10,
    "minSalary": 6000
  }
}
```

### 8.4 请求字段说明

| 字段 | 是否必填 | 说明 |
|---|---:|---|
| `tenant` | 是 | 租户 |
| `apiCode` | 是 | 接口编码 |
| `token` | 是 | 接口授权 token |
| `systemCode` | 是 | 调用方系统编码 |
| `params` | 否 | 只允许传 `IN/INOUT` 参数 |
| `exportExcel` | 否 | `true` 表示导出 Excel |

### 8.5 参数传递规则

存储过程执行时：

- 只校验 `IN` 和 `INOUT` 参数。
- 调用方不能传 `OUT` 参数。
- 多传未知参数会报错。
- 必填参数缺失会报错。
- 必填参数传 `null` 或空字符串会报错。
- 非必填参数可以不传。
- 非必填参数可以显式传 `null`。

显式传 `null` 时，后端会按 `jdbcType` 调用：

```java
CallableStatement.setNull(index, sqlType)
```

这是为 Oracle 兼容做的修改，避免 `setObject(index, null)` 在 Oracle 驱动下报 `Invalid column type`。

## 9. 执行返回结构

### 9.1 标准返回

存储过程执行成功后，业务数据结构为：

```json
{
  "outParams": {
    "status": "0",
    "message": "success"
  },
  "cursors": {
    "data": [
      {
        "emp_id": 1,
        "emp_name": "张三",
        "salary": 5000
      }
    ]
  },
  "resultCount": 1
}
```

如果经过 Controller 包装，接口整体响应通常为：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "outParams": {},
    "cursors": {},
    "resultCount": 0
  }
}
```

具体外层字段以项目 `ResultModel` 实际结构为准。

### 9.2 字段说明

| 字段 | 说明 |
|---|---|
| `outParams` | 普通 `OUT/INOUT` 标量输出 |
| `cursors` | `OUT SYS_REFCURSOR` 游标输出 |
| `resultCount` | 所有游标行数合计 |

### 9.3 OUT 标量

Oracle 过程：

```sql
p_status OUT VARCHAR2
```

参数配置：

```json
{
  "code": "status",
  "direction": 2,
  "jdbcType": "VARCHAR",
  "orderNo": 3
}
```

返回：

```json
{
  "outParams": {
    "status": "0"
  }
}
```

### 9.4 INOUT 参数

Oracle 过程：

```sql
p_salary IN OUT NUMBER
```

参数配置：

```json
{
  "code": "salary",
  "direction": 3,
  "jdbcType": "DECIMAL",
  "orderNo": 1
}
```

请求：

```json
{
  "params": {
    "salary": 5000
  }
}
```

返回：

```json
{
  "outParams": {
    "salary": 10000
  }
}
```

### 9.5 OUT 游标

Oracle 过程：

```sql
p_data OUT SYS_REFCURSOR
```

参数配置：

```json
{
  "code": "data",
  "direction": 2,
  "jdbcType": "CURSOR",
  "orderNo": 5
}
```

返回：

```json
{
  "cursors": {
    "data": [
      {
        "emp_id": 1,
        "emp_name": "张三"
      }
    ]
  }
}
```

前端展示建议：

- `outParams` 展示为键值区。
- `cursors` 每个 key 展示为一个表格。
- 多个游标时按 Tab 展示，例如 `deptData`、`topData`。

## 10. Excel 导出

### 10.1 请求方式

仍然调用：

```http
POST /api/actuator/execute
```

请求中增加：

```json
{
  "exportExcel": true
}
```

示例：

```json
{
  "exportExcel": true,
  "tenant": "glxt",
  "apiCode": "procEmpQuery",
  "token": "接口分组token",
  "systemCode": "glxt-test",
  "params": {
    "deptId": 10
  }
}
```

### 10.2 存储过程导出规则

| 存储过程返回 | 导出行为 |
|---|---|
| 一个游标 | 导出该游标表格 |
| 没有游标 | 导出 `outParams` 键值表 |
| 多个游标 | 返回错误 |

多游标错误文案：

```text
存储过程多游标结果暂不支持Excel导出
```

前端注意：

- 普通执行时多游标支持展示。
- Excel 导出时多游标当前不支持。
- 如果要导出多个游标，需要后续扩展为多 Sheet 导出。

## 11. 执行日志变化

本次对存储过程执行日志做了适配：

| 字段 | 存储过程逻辑 |
|---|---|
| `EXECUTE_SQL` | 保存 `{ call proc_name(?, ?) }` |
| `RESULT_COUNT` | 保存所有游标行数合计 |

注意：

- 没有游标时 `RESULT_COUNT=0`。
- 执行失败时，也会尽量保留 `{ call ... }` 调用文本，方便排查。

## 12. 前端需要重点适配的修改点

### 修改点 1：新增接口类型

接口类型下拉框增加：

```text
存储过程 = 2
```

当选择存储过程时，切换页面字段和参数表格。

### 修改点 2：新增存储过程名称

新增字段：

```json
{
  "procedureName": "proc_emp_query"
}
```

该字段仅存储过程类型必填。

### 修改点 3：参数表格新增三列

新增：

```text
direction
jdbcType
orderNo
```

存储过程类型下必须配置这三个字段。

### 修改点 4：OUT 参数不作为入参展示

在 demo、执行参数输入区：

- 展示 `IN`。
- 展示 `INOUT`。
- 不展示 `OUT`。

### 修改点 5：执行结果展示结构变化

SQL 查询接口通常返回列表或分页对象。

存储过程接口返回：

```text
outParams + cursors + resultCount
```

前端需要根据返回结构渲染：

- `outParams`：键值表。
- `cursors`：结果表格。
- 多游标：多个表格或 Tab。

### 修改点 6：Excel 导出错误处理

多游标导出会失败，前端直接提示后端错误文案。

### 修改点 7：接口配置导出仍按 ids

注意区分两个导出：

| 功能 | 接口 | 参数 |
|---|---|---|
| 执行结果导出 Excel | `/api/actuator/execute` | `exportExcel=true` |
| 接口配置导出 Excel | `/api/interface/export` | `ids` |

接口配置导出不支持用 `tenant/type/code` 过滤导出，只读取：

```json
{
  "ids": [1001, 1002]
}
```

## 13. 前端校验建议

保存前建议校验：

| 校验项 | 规则 |
|---|---|
| `type` | 必选 |
| `procedureName` | `type=2` 时必填 |
| `connectionId` | `type=2` 时必填 |
| `apiParamList` | 根据过程实际参数配置 |
| `direction` | 过程参数必填 |
| `jdbcType` | 过程参数必填 |
| `orderNo` | 过程参数必填且正整数 |
| `orderNo` | 同一接口内不能重复 |
| `CURSOR` | 只能配置为 `OUT` |
| `IN/INOUT` | 必须配置 `required/validateType` |
| `OUT` | 不要求入参校验字段 |

执行前建议校验：

| 校验项 | 规则 |
|---|---|
| `tenant` | 必填 |
| `apiCode` | 必填 |
| `token` | 必填 |
| `systemCode` | 必填 |
| `params` | 只能包含 `IN/INOUT` 参数 |
| 必填参数 | 不能缺失、不能为 `null`、不能为空字符串 |

## 14. 典型错误和处理

| 错误场景 | 原因 | 前端处理 |
|---|---|---|
| 参数绑定错位 | `orderNo` 与 Oracle 参数顺序不一致 | 配置页突出展示参数顺序，保存前校验 |
| OUT 参数被传入 | 调用方传了 `OUT` 参数 | 前端执行表单不要渲染 OUT 参数 |
| Oracle 空值报错 | 使用 `setObject(null)` 可能触发 | 后端已改为 `setNull`；前端可允许非必填参数传 null |
| 多游标导出失败 | 当前只支持单游标导出 | 提示“多游标暂不支持导出” |
| 游标方向配置错误 | `CURSOR` 配成 `IN/INOUT` | 前端限制 `CURSOR` 只能选 `OUT` |
| 配置导出参数无效 | `/api/interface/export` 只读 `ids` | 使用 `ids` 导出 |

## 15. 推荐页面交互

### 15.1 配置页

当 `type=2`：

- 显示“存储过程配置”区域。
- 隐藏 SQL 拼接区域。
- 参数表格显示 `orderNo/direction/jdbcType`。
- `jdbcType=CURSOR` 时自动锁定 `direction=OUT`。
- 支持“按顺序重排”按钮，自动生成 `1,2,3...`。

### 15.2 执行结果页

存储过程结果建议分三块：

```text
执行状态/OUT参数
游标结果集
执行统计
```

示例：

```text
OUT参数：
status = 0
message = success

游标：
data 表格

统计：
resultCount = 2
```

### 15.3 多游标展示

返回：

```json
{
  "cursors": {
    "deptData": [],
    "topData": []
  }
}
```

建议 UI：

```text
[deptData] [topData]
```

每个 Tab 展示对应表格。

## 16. 与旧 SQL 查询接口兼容性

本次改动保持以下兼容：

- SQL 查询接口保存逻辑不变。
- SQL 查询接口执行请求体不变。
- SQL 查询接口分页逻辑不变。
- SQL 查询接口 Excel 导出逻辑不变。
- 旧数据 `type` 为空时仍按 SQL 查询处理。

前端不要强制要求历史 SQL 接口必须补 `type=1`，否则可能影响旧数据编辑体验。

## 17. 联调建议

建议按以下顺序联调：

1. 保存一个 `INOUT NUMBER` 过程，验证 `outParams`。
2. 保存一个 `OUT SYS_REFCURSOR` 过程，验证 `cursors`。
3. 保存一个多个 `OUT` 标量过程，验证键值返回。
4. 验证必填参数缺失、类型错误、未知参数。
5. 验证非必填参数传 `null`。
6. 验证单游标 Excel 导出。
7. 验证无游标 Excel 导出。
8. 验证多游标 Excel 导出失败提示。
9. 验证执行记录中的 `{ call ... }` 和 `resultCount`。

可直接使用以下测试脚本：

- [double-value-api-test.http](D:/projects/glxt-api/docs/double-value-api-test.http)
- [employees-procedure-api-test.http](D:/projects/glxt-api/docs/employees-procedure-api-test.http)

