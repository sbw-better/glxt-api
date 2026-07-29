# 存储过程接口新增页面设计

## 1. 设计目标

在现有“SQL接口新增”弹窗基础上扩展“存储过程接口新增”能力。页面保持原有新增弹窗的整体布局、按钮位置和参数表格风格，避免前端重新设计整套页面。

当用户选择：

- `接口类型 = SQL查询`：展示现有 SQL 配置字段，保持原功能不变。
- `接口类型 = 存储过程`：隐藏 SQL 拼接相关字段，展示存储过程名称、数据源、过程参数配置、调用预览等字段。

后端接口继续复用现有接口：

- 保存：`POST /api/interface/save`
- 预览：`POST /api/interface/preview`
- 调用 demo：`POST /api/interface/demo`
- 执行：`POST /api/actuator/execute`

## 2. 页面入口

现有列表页点击“新增”后打开弹窗。

弹窗标题建议根据接口类型动态展示：

- 初始：`新增`
- SQL 查询：`新增 - SQL查询接口`
- 存储过程：`新增 - 存储过程接口`

如果接口类型默认值继续为空，建议前端默认选中 `SQL查询`，兼容旧配置习惯。

## 3. 页面整体布局

沿用截图中的双列表单布局：

```text
┌────────────────────────────────────────────────────────────┐
│ 新增 - 存储过程接口                                  X      │
├────────────────────────────────────────────────────────────┤
│ 基础信息                                                   │
│ 接口名称      [                        ]  接口类型 [存储过程] │
│ 接口代码      [                        ]  排序号   [        ] │
│ 接口描述      [                                            ] │
│ 数据源ID      [请选择数据源                                ] │
│ 存储过程名称  [PKG_NAME.PROC_NAME                         ] │
│ 字段返回模式  [默认模式/别名模式                           ] │
│ 是否分页      [否，只读/隐藏]                               │
│                                                            │
│ [调用预览]  { call PKG_NAME.PROC_NAME(?, ?, ?) }             │
├────────────────────────────────────────────────────────────┤
│ 过程参数                                                   │
│ [新增参数]                                                   │
│ ┌ 参数表格                                                ┐ │
│ │ 顺序 参数名称 参数代码 方向 参数类型 JDBC类型 必填 校验...│ │
│ └────────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────┤
│                                            [确定] [取消]    │
└────────────────────────────────────────────────────────────┘
```

## 4. 基础信息区域

### 4.1 通用字段

| 字段 | 控件 | 是否必填 | 说明 |
|---|---|---:|---|
| 接口名称 `name` | 输入框 | 是 | 与 SQL 接口一致 |
| 接口代码 `code` | 输入框 | 是 | 唯一标识，保存后尽量不允许随意修改 |
| 接口类型 `type` | 下拉框 | 是 | `1=SQL查询`，`2=存储过程` |
| 排序号 `orderNo` | 数字输入框 | 否 | 列表排序使用 |
| 接口描述 `remark/desc` | 输入框/文本域 | 否 | 与现有页面保持一致 |
| 数据源ID `connectionId` | 下拉框 | 是 | 存储过程必须指定数据源 |
| 字段返回模式 `fieldBackMode` | 下拉框 | 建议必填 | 游标结果字段返回时复用该配置 |

### 4.2 存储过程专属字段

| 字段 | 控件 | 是否必填 | 说明 |
|---|---|---:|---|
| 存储过程名称 `procedureName` | 输入框 | 是 | 支持 `SCHEMA.PKG.PROC`、`PKG.PROC`、`SCHEMA.PROC` |
| 是否分页 `page` | 固定为否 | 否 | 存储过程本身不走 SQL 分页逻辑 |
| 调用预览 | 只读文本框 | 否 | 调用 `/api/interface/preview` 返回 `{ call ... }` |

`procedureName` 前端建议提示：

```text
请输入完整存储过程名，例如：PKG_FUND_QUERY.QUERY_FUND_INFO
如跨 schema 调用，可填写：GLXT.PKG_FUND_QUERY.QUERY_FUND_INFO
```

前端可做基础字符校验：

```text
仅允许字母、数字、下划线、点、$、#
```

对应后端校验规则：

```text
[A-Za-z0-9_.$#]+
```

## 5. SQL 字段隐藏规则

当 `type=2` 存储过程时，隐藏或禁用以下 SQL 查询字段：

| 原 SQL 字段 | 存储过程页面处理 |
|---|---|
| `selectParam` | 隐藏 |
| `fromParam` | 隐藏 |
| `whereParamFixed` | 隐藏 |
| `whereParamChange` | 隐藏 |
| `groupParam` | 隐藏 |
| `orderParam` | 隐藏 |
| `managerField` | 隐藏或禁用 |
| `fundIdsField` | 隐藏或禁用 |
| `fundCodesField` | 隐藏或禁用 |
| `page` | 固定为否 |
| SQL 预览按钮 | 改为“调用预览” |

说明：存储过程不通过 `concatSql` 拼接 SQL，参数也不参与 `@IN/@EQ/@LIKE` 这类 SQL 模板语法。

## 6. 过程参数表格设计

### 6.1 表格列

建议将现有“接口参数”表格替换为“过程参数”表格：

| 列名 | 字段 | 控件 | 是否必填 | 说明 |
|---|---|---|---:|---|
| 顺序 | `orderNo` | 数字输入框 | 是 | 参数绑定顺序，从 1 开始 |
| 参数名称 | `name` | 输入框 | 是 | 中文名称或业务名称 |
| 参数代码 | `code` | 输入框 | 是 | 请求/返回 JSON 中使用的 key |
| 参数方向 | `direction` | 下拉框 | 是 | `1=IN`，`2=OUT`，`3=INOUT` |
| 参数类型 | `type` | 下拉框 | 建议必填 | IN/INOUT 表示入参业务类型；OUT 标量表示返回值业务类型；OUT CURSOR 固定为列表 |
| JDBC类型 | `jdbcType` | 下拉框 | 是 | 见下方类型枚举 |
| 是否必填 | `required` | 下拉框 | IN/INOUT必填 | OUT 参数不展示或禁用 |
| 默认值 | `defaultValue` | 输入框 | 否 | 仅 IN/INOUT 有意义 |
| 校验类型 | `validateType` | 下拉框 | IN/INOUT必填 | OUT 参数不展示或禁用 |
| 校验表达式 | `expression` | 输入框 | 条件必填 | 正则/表达式校验时必填 |
| 校验说明 | `error` | 输入框 | 条件必填 | 校验失败提示 |
| 操作 | - | 编辑/删除按钮 | - | 行操作 |

### 6.2 参数类型枚举

| 展示值 | 提交值 | 含义 |
|---|---:|---|
| 字符串 | `1` | 文本入参 |
| 整型 | `2` | 整数入参 |
| 浮点数 | `3` | 数值型入参，沿用原 `FILED_TYPE_FLOAT` |
| 日期 | `4` | 日期入参 |
| 列表 | `5` | 数组/列表入参 |

### 6.3 参数方向枚举

| 展示值 | 提交值 | 含义 | 调用方是否传入 |
|---|---:|---|---|
| IN | `1` | 输入参数 | 是 |
| OUT | `2` | 输出参数 | 否 |
| INOUT | `3` | 输入输出参数 | 是 |

交互规则：

- 选择 `OUT` 后，`required/defaultValue/validateType/expression/error` 禁用或隐藏。
- 选择 `OUT` 后，参数类型仍展示；标量 OUT 用于说明返回值类型，CURSOR 固定为列表。
- 选择 `IN` 或 `INOUT` 后，展示入参校验相关字段。
- 请求体 `params` 中只允许出现 `IN/INOUT` 参数。
- `OUT` 参数由后端从 `CallableStatement` 读取并返回。

### 6.4 JDBC 类型枚举

| 展示值 | 提交值 | 适用 Oracle 类型 |
|---|---|---|
| VARCHAR | `VARCHAR` | `VARCHAR2`、`CHAR` |
| INTEGER | `INTEGER` | `NUMBER(10)` 等 |
| BIGINT | `BIGINT` | 大整数 `NUMBER` |
| DECIMAL | `DECIMAL` | `NUMBER(p,s)` |
| DATE | `DATE` | `DATE` |
| TIMESTAMP | `TIMESTAMP` | `TIMESTAMP` |
| CURSOR | `CURSOR` | `SYS_REFCURSOR` |

交互规则：

- `CURSOR` 一期只允许 `direction=OUT`。
- 如果用户选择 `jdbcType=CURSOR`，自动将方向设为 `OUT`，并禁用方向修改，或在保存前提示。
- `DATE` 支持输入格式：`yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss`。
- `TIMESTAMP` 支持输入格式：`yyyy-MM-dd HH:mm:ss`。

### 6.4 ORDER_NO 说明

`ORDER_NO` 是存储过程参数的绑定顺序。

Oracle 调用使用 `CallableStatement` 按位置绑定参数，而不是按参数名绑定。例如：

```sql
PROCEDURE QUERY_FUND(
  P_FUND_CODE IN VARCHAR2,
  P_STATUS OUT VARCHAR2,
  P_DATA OUT SYS_REFCURSOR
)
```

页面应配置为：

| orderNo | code | direction | jdbcType |
|---:|---|---|---|
| 1 | fundCode | IN | VARCHAR |
| 2 | status | OUT | VARCHAR |
| 3 | data | OUT | CURSOR |

后端实际调用：

```sql
{ call QUERY_FUND(?, ?, ?) }
```

如果 `ORDER_NO` 配错，即使参数代码正确，也会出现绑定错位。

前端校验建议：

- `orderNo` 必填。
- `orderNo` 必须为正整数。
- 同一个接口内 `orderNo` 不允许重复。
- 保存前按 `orderNo` 升序展示参数，方便用户核对。

## 7. 调用预览区域

建议在参数表格上方或下方提供“调用预览”按钮。

点击后调用：

```http
POST /api/interface/preview
```

请求示例：

```json
{
  "type": 2,
  "procedureName": "PKG_FUND_QUERY.QUERY_FUND",
  "apiParamList": [
    {
      "code": "fundCode",
      "direction": 1,
      "jdbcType": "VARCHAR",
      "orderNo": 1
    },
    {
      "code": "status",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 2
    },
    {
      "code": "data",
      "direction": 2,
      "jdbcType": "CURSOR",
      "orderNo": 3
    }
  ]
}
```

返回示例：

```text
{ call PKG_FUND_QUERY.QUERY_FUND(? /* fundCode:IN:VARCHAR */, ? /* status:OUT:VARCHAR */, ? /* data:OUT:CURSOR */) }
```

页面展示建议：

- 使用只读多行文本框。
- 参数较多时允许横向滚动。
- 保存前如果预览未生成，不强制阻断，但建议提示用户核对参数顺序。

## 8. 保存请求结构

```json
{
  "name": "基金信息查询过程",
  "code": "queryFundByProc",
  "type": 2,
  "orderNo": 10,
  "connectionId": 1001,
  "fieldBackMode": 1,
  "page": 0,
  "procedureName": "PKG_FUND_QUERY.QUERY_FUND",
  "apiParamList": [
    {
      "name": "基金代码",
      "code": "fundCode",
      "direction": 1,
      "jdbcType": "VARCHAR",
      "orderNo": 1,
      "required": 1,
      "defaultValue": null,
      "validateType": 0
    },
    {
      "name": "执行状态",
      "code": "status",
      "direction": 2,
      "jdbcType": "VARCHAR",
      "orderNo": 2
    },
    {
      "name": "结果游标",
      "code": "data",
      "direction": 2,
      "jdbcType": "CURSOR",
      "orderNo": 3
    }
  ]
}
```

## 9. 调用 demo 展示

存储过程接口的调用 demo 中只展示 `IN/INOUT` 参数，不展示 `OUT` 参数。

```json
{
  "token": "替换为接口所在分组的token值",
  "systemCode": "替换为接口调用方的系统代码，如\"glxt\"",
  "tenant": "glxt",
  "apiCode": "queryFundByProc",
  "params": {
    "fundCode": "XXX"
  }
}
```

## 10. 执行返回展示

普通执行返回：

```json
{
  "outParams": {
    "status": "0"
  },
  "cursors": {
    "data": [
      {
        "fund_code": "A001",
        "fund_name": "测试基金"
      }
    ]
  },
  "resultCount": 1
}
```

前端展示建议：

- `outParams`：使用键值表展示。
- `cursors`：每个游标展示一个结果表格。
- 多游标时使用 Tab：Tab 名称取参数 `code`，例如 `data`、`detailList`。
- `resultCount`：展示为游标总行数。

## 11. Excel 导出交互

存储过程导出继续使用：

```json
{
  "exportExcel": true,
  "tenant": "glxt",
  "apiCode": "queryFundByProc",
  "token": "...",
  "systemCode": "glxt",
  "params": {
    "fundCode": "A001"
  }
}
```

前端提示规则：

- 一个游标：导出该游标。
- 没有游标：导出 `outParams` 键值表。
- 多个游标：后端返回错误 `存储过程多游标结果暂不支持Excel导出`，前端直接提示。

## 12. 页面校验清单

保存前前端建议校验：

- 接口类型必选。
- 存储过程类型必须填写 `procedureName`。
- 存储过程类型必须选择 `connectionId`。
- 至少配置一个参数；如果无参数过程允许为空，则需要产品确认。
- 每个参数建议填写 `name/code/type/direction/jdbcType/orderNo`。
- `orderNo` 不允许重复。
- `CURSOR` 只能为 `OUT`。
- `IN/INOUT` 参数必须配置 `required/validateType`，并用 `type` 表示入参业务类型。
- `OUT` 标量参数用 `type` 表示返回值业务类型；`OUT CURSOR` 的 `type` 固定为列表。
- `OUT` 参数不要求 `required/defaultValue/validateType/expression/error`。
- 请求 demo 和执行请求中不能出现 `OUT` 参数。

## 13. 推荐交互细节

- 接口类型切换时弹确认：
  - 从 SQL 查询切到存储过程：提示“切换后 SQL 配置项将不参与保存校验，请确认”。
  - 从存储过程切到 SQL 查询：提示“切换后存储过程配置项将不参与保存校验，请确认”。
- 参数表格不提供上移、下移、批量删除；排序直接维护 `orderNo`。
- `procedureName` 下方展示示例：`PKG_NAME.PROC_NAME`。
- `CURSOR` 参数行使用标签突出展示，方便用户识别结果集参数。
- 调用预览区域固定在参数表格上方，参数配置完整后自动刷新；未配置完整时提示“请先完善过程参数”。

## 14. 与现有 SQL 新增页的差异

| 区域 | SQL 查询接口 | 存储过程接口 |
|---|---|---|
| 核心配置 | `select/from/where/group/order` | `procedureName` |
| 参数含义 | SQL 模板入参 | CallableStatement 位置参数 |
| 参数顺序 | 不严格依赖 | 严格依赖 `ORDER_NO` |
| 返回结果 | List/PageInfoResult | `outParams + cursors` |
| Excel 导出 | 查询结果导出 | 单游标或 outParams 导出 |
| 分页 | 支持 | 一期不支持 |
| 字段鉴权参数 | 支持 | 一期不参与过程调用 |

## 15. 建议页面开发步骤

1. 在接口类型下拉框增加 `存储过程(type=2)`。
2. 根据 `type` 控制 SQL 配置区和存储过程配置区显示。
3. 新增 `procedureName` 表单字段。
4. 参数表格保留原 `type` 参数类型列，并扩展 `direction/jdbcType/orderNo`。
5. 增加过程参数的前端校验。
6. 调整 demo 生成展示：过滤 `OUT` 参数。
7. 调整执行结果展示：支持 `outParams/cursors/resultCount`。
8. 调整 Excel 导出错误提示：识别多游标不支持的提示文案。
