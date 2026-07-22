# API 接口配置、查询与执行逻辑说明

适用项目：`glxt-api`

文档日期：2026-07-22

## 1. 总览

`glxt-api` 中的 API 接口能力，本质上是一套“接口配置驱动 SQL 查询”的机制：

1. 在接口配置模块中维护接口元数据、SQL 片段、入参定义、分页配置、字段返回模式和数据源。
2. 在授权模块中维护 token、IP 白名单、授权分组与接口的关系。
3. 调用方通过 `/api/actuator/execute` 传入 `tenant`、`apiCode`、`token`、`params` 等信息。
4. 服务端根据 `tenant + apiCode` 查出接口配置，校验权限和参数，拼接 SQL，切换数据源并执行查询。
5. 执行完成后，将入参、执行 SQL、结果数量、耗时和执行状态写入执行记录表。

核心代码分布如下：

| 领域 | 入口或实现 | 说明 |
| --- | --- | --- |
| 接口配置 Controller | `src/main/java/com/citics/glxtapi/web/controller/ApiInterfaceController.java` | `/api/interface` 下的配置 CRUD、预览、导入导出 |
| 接口配置 Service | `src/main/java/com/citics/glxtapi/web/service/impl/ApiServiceImpl.java` | 接口配置校验、保存、查询、预览 SQL |
| 接口入参 Service | `src/main/java/com/citics/glxtapi/web/service/impl/ApiParamServiceImpl.java` | 入参保存、必填判断、默认值补充、参数查询 |
| 接口执行 Controller | `src/main/java/com/citics/glxtapi/web/controller/ApiActuatorController.java` | `/api/actuator/execute`、执行记录查询 |
| 接口执行 Service | `src/main/java/com/citics/glxtapi/web/service/impl/ApiActuatorServiceImpl.java` | 执行前校验、SQL 拼接、数据源切换、查询、日志 |
| 授权 Service | `src/main/java/com/citics/glxtapi/web/service/impl/TenantAuthServiceImpl.java` | token、IP、接口授权关系校验 |
| 数据源 Service | `src/main/java/com/citics/glxtapi/web/service/impl/ConnectionServiceImpl.java` | 数据源配置维护、动态数据源同步 |
| SQL 执行模块 | `src/main/java/com/citics/glxtapi/plugin/sql/DbModule.java` | SQL 执行、分页、count 查询、字段返回模式 |
| SQL 占位符解析 | `src/main/java/com/citics/glxtapi/plugin/sql/parse/TextSqlNode.java` | `#{}`、`${}`、`?{}`、`@IN{}`、`@LIKE{}` 等解析 |
| 动态数据源 | `src/main/java/com/citics/glxtapi/plugin/db/DynamicRoutingDataSource.java` | 根据上下文切换实际 `DataSource` |

## 2. 核心数据模型

### 2.1 接口主配置：`API_SQL_INTERFACE`

实体：`ApiInterface`

| 字段 | 说明 |
| --- | --- |
| `ID` | 接口配置主键 |
| `TENANT` | 租户编码 |
| `NAME` | 接口名称 |
| `TYPE` | 接口类型 |
| `CODE` | 接口代码，执行时通过 `apiCode` 匹配 |
| `DESCRIPTION` | 接口描述 |
| `ORDER_NO` | 排序号 |
| `SELECT_PARAM` | SQL 的 SELECT 片段 |
| `ZDFHMS` | 字段返回模式，默认小写或按别名原样返回 |
| `FROM_PARAM` | SQL 的 FROM 片段 |
| `WHERE_PARAM_FIXED` | 固定 WHERE 条件 |
| `WHERE_PARAM_CHANGE` | 可变 WHERE 条件 |
| `GROUP_PARAM` | GROUP BY 片段 |
| `ORDER_PARAM` | ORDER BY 片段 |
| `PAGE` | 是否默认分页，`1` 是，`0` 否 |
| `CONNECTION_ID` | 数据源 ID，`0` 表示默认数据源 |
| `MANAGER_FIELD` | 管理人鉴权字段 |
| `FUND_IDS_FIELD` | 产品 ID 鉴权字段 |
| `FUND_CODES_FIELD` | 产品代码鉴权字段 |
| `PREVIEW_SQL` | 保存配置时生成的预览 SQL |
| `CREATE_TIME` / `CREATE_BY` | 创建信息 |
| `UPDATE_TIME` / `UPDATE_BY` | 更新信息 |

### 2.2 接口入参配置：`API_SQL_INTERFACE_PARAM`

实体：`ApiParam`

| 字段 | 说明 |
| --- | --- |
| `ID` | 参数主键 |
| `API_ID` | 所属接口 ID |
| `NAME` | 参数名称 |
| `CODE` | 参数编码，执行时与 `params` 中的 key 匹配 |
| `TYPE` | 参数类型，`1` 字符串，`2` 整型，`3` 浮点型，`4` 日期型，`5` 数组 |
| `REQUIRED` | 是否必填，`1` 是，`0` 否 |
| `DEFAULT_VALUE` | 非必填参数默认值 |
| `VALIDATE_TYPE` | 校验方式，`1` 不校验，`2` 表达式校验，`3` 正则校验 |
| `EXPRESSION` | 校验表达式或正则表达式 |
| `ERROR` | 校验失败提示 |
| `DESCRIPTION` | 参数说明 |

### 2.3 执行记录：`API_SQL_ACTUATOR`

实体：`ApiActuator`

| 字段 | 说明 |
| --- | --- |
| `ID` | 执行记录主键 |
| `TENANT` | 租户 |
| `SYSTEM_CODE` | 调用方系统编码 |
| `API_ID` | 接口 ID |
| `API_CODE` | 接口编码 |
| `INTER_PARAM` | 原始入参 JSON |
| `INTER_IP` | 调用方 IP |
| `EXECUTE_TIME` | 执行时间 |
| `EXECUTE_BY` | 执行人 |
| `EXECUTE_RESULT` | 执行结果，`1` 成功，`0` 失败且隐藏错误，`-1` 失败 |
| `EXECUTE_RESULT_DETAIL` | 执行结果详情或错误信息 |
| `EXECUTE_SQL` | 最终执行 SQL，来自 `SqlContextHolder` |
| `EXECUTE_CONSUME` | 执行耗时 |
| `RESULT_COUNT` | SQL 查询结果数量 |

### 2.4 授权相关表

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `API_SQL_TENANT_AUTH` | `TenantAuth` | 租户授权分组，维护 token、IP 白名单 |
| `API_SQL_TENANT_INTERFACE` | `TenantInterface` | 授权分组与接口的关联关系 |

执行时必须满足：

1. 请求中存在 `token`。
2. token 可以匹配到授权分组。
3. 如果授权分组配置了 `IP_CONFIG`，调用 IP 必须命中白名单。
4. 授权分组必须关联当前执行接口。

### 2.5 数据源配置表：`API_SQL_CONNECTION`

实体：`Connection`

| 字段 | 说明 |
| --- | --- |
| `ID` | 数据源 ID |
| `NAME` | 数据源名称 |
| `TYPE` | 数据源类型，目前主要是 `db` |
| `CONFIG` | 连接配置 JSON |
| `EXTEND_CONFIG` | 扩展配置 JSON |
| `TIMEOUT` | 超时时间 |
| `CODE` | 数据源编码 |
| `TENANT` | 所属租户 |

接口配置中的 `CONNECTION_ID` 指向该表。执行时会将数据源切换到：

```text
connection.code + "_master"
```

如果 `CONNECTION_ID = 0`，则使用默认数据源。

## 3. 接口配置逻辑

### 3.1 配置入口

Controller：`ApiInterfaceController`

统一路径：`/api/interface`

| 接口 | 方法 | 说明 | 最低角色 |
| --- | --- | --- | --- |
| `/save` | POST | 保存或更新接口配置及入参 | `developer` |
| `/delete` | POST | 删除接口配置 | `maintainer` |
| `/get` | POST | 查询单条接口配置及入参 | `guest` |
| `/list` | POST | 列表查询 | `guest` |
| `/page` | POST | 分页查询 | `guest` |
| `/preview` | POST | 接口 SQL 预览 | `guest` |
| `/demo` | POST | 生成 HTTP 调用 demo | `developer` |
| `/have_page` | POST | 判断接口是否分页 | 无 `ApiPreHandle` 注解 |
| `/export` | POST | 导出接口配置 Excel | `developer` |
| `/upload` | POST | 导入接口配置 Excel | `developer` |

说明：`ApiPreHandle` 注解本项目内只有定义，未看到本项目内的切面或拦截器实现。角色、租户、用户上下文可能由外部公共组件、网关或部署侧注入。

### 3.2 保存接口配置

入口方法：`ApiServiceImpl.save(ApiInterfaceDTO dto)`

主要流程：

1. 校验 `apiParamList` 必传，可以为空数组，但字段不能缺失。
2. 获取当前租户：
   - 优先使用 `tenantService.getTenant()`。
   - 如果上下文中没有租户，则使用 DTO 中的 `tenant`。
3. 校验接口主配置：
   - `name` 不为空。
   - `code` 不为空。
   - `selectParam` 不为空。
   - `fieldBackMode` 不为空。
   - `fromParam` 不为空。
   - `page` 不为空。
   - `connectionId` 不为空。
   - 当前租户必须拥有该数据源权限。
4. 校验每个入参：
   - `name`、`code`、`type`、`required`、`validateType` 必填。
   - 如果不是“不校验”，则 `expression` 和 `error` 必填。
   - 特殊鉴权参数 `managerField`、`fundIdsField`、`fundCodesField` 必须配置为非必填。
   - `managerField` 类型必须是整型。
   - `fundIdsField`、`fundCodesField` 类型必须是字符串。
5. 调用 `encode(...)` 将 DTO 转为 `ApiInterface`。
6. `encode(...)` 内会调用 `preview(dto)` 生成 `previewSql`。
7. 新增时校验同租户下 `code` 不重复，并写入创建人、创建时间。
8. 更新时校验旧记录存在，并写入更新人、更新时间。
9. 调用 `apiParamService.save(apiInterface.getId(), apiParamList)` 保存入参。

入参保存逻辑：

1. 查询接口旧参数。
2. 前端未传回的旧参数会被删除。
3. 新参数校验同接口下 `code` 不重复后新增。
4. 旧参数按 ID 更新。

### 3.3 删除接口配置

入口方法：`ApiServiceImpl.delete(Serializable id)`

删除内容：

1. 删除 `API_SQL_INTERFACE` 主配置。
2. 删除 `API_SQL_INTERFACE_PARAM` 中的接口参数。
3. 删除 `API_SQL_TENANT_INTERFACE` 中与该接口相关的授权关系。

## 4. 接口配置查询逻辑

### 4.1 查询单条配置

入口方法：`ApiServiceImpl.get(Long id, String code)`

规则：

1. `id` 和 `code` 不能同时为空。
2. 如果当前租户上下文不为空，则查询条件追加 `tenant = 当前租户`。
3. 如果传了 `id`，按 ID 精确匹配。
4. 如果传了 `code`，按 code 精确匹配。
5. 查询到接口主配置后，再查参数列表。
6. 返回 `ApiInterfaceVO`，包含接口主信息和 `apiParamList`。

### 4.2 执行前按接口编码查询

入口方法：`ApiServiceImpl.getByApi(String tenant, String code)`

该方法用于执行接口时定位配置。

规则：

1. 按 `tenant` 和 `code` 精确查询。
2. 如果不存在，直接抛出错误。
3. 查询到主配置后，加载参数列表。
4. 返回 `ApiInterfaceVO`。

执行请求中的 `tenant` 和 `apiCode` 会走这个方法，因此接口执行依赖 `tenant + apiCode` 唯一定位。

### 4.3 列表查询

入口方法：`ApiServiceImpl.list(ApiInterfaceDTO dto)`

支持条件：

| 条件 | 匹配方式 |
| --- | --- |
| 当前租户 | 精确匹配 |
| `name` | 模糊匹配 |
| `code` | 精确匹配 |
| `type` | 精确匹配 |
| `page` | 精确匹配 |
| `connectionId` | 精确匹配 |

排序：

1. `order_no` 升序。
2. `id` 升序。

### 4.4 分页查询

入口方法：`ApiServiceImpl.page(ApiInterfaceDTO dto)`

支持条件：

| 条件 | 匹配方式 |
| --- | --- |
| 当前租户 | 精确匹配 |
| `name` | 模糊匹配 |
| `code` | 不区分大小写的模糊匹配 |
| `type` | 精确匹配 |
| `page` | 精确匹配 |
| `connectionId` | 精确匹配 |
| `description` | 模糊匹配 |
| `previewSql` | 去空格、不区分大小写的模糊匹配 |

分页参数来自 DTO 的 `pageNum`、`pageSize`，通过 `PageFactory.jsonPage(...)` 构造 MyBatis Plus 分页对象。

## 5. SQL 预览逻辑

入口方法：`ApiServiceImpl.preview(ApiInterfaceDTO dto)`

预览 SQL 的拼接顺序：

```text
SELECT selectParam
FROM fromParam
WHERE ( 1 = 1 )
[AND managerField = #{managerField}]
[AND @IN{fundIdsField, fundIdsFieldColumn}]
[AND @IN{fundCodesField, fundCodesFieldColumn}]
[AND (whereParamFixed)]
[AND (whereParamChange)]
[GROUP BY groupParam]
[ORDER BY orderParam]
```

预览方法还会把占位符替换成更适合前端展示的文字：

| 原语法 | 预览效果 |
| --- | --- |
| `#{name}` | 展示为变量占位 |
| `@IN{key,col}` | 展示为 `col IN (变量 key)` |
| `@LIKE{key,col}` | 展示为 `col LIKE (%key%)` |
| `@LIKER{key,col}` | 展示为右模糊 |
| `@LIKEL{key,col}` | 展示为左模糊 |
| `?{param,sql}` | 展示为条件说明 |

注意：当前代码中，预览 SQL 会拼接 `whereParamChange`，但实际执行 SQL 中 `whereParamChange` 逻辑被注释，未启用。

## 6. 接口执行逻辑

### 6.1 执行入口

Controller：`ApiActuatorController`

统一路径：`/api/actuator`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/execute` | POST | 执行接口 SQL；可通过 `exportExcel=true` 导出 Excel |
| `/execute_check` | POST | 测试 SQL 前校验结果数量 |
| `/page` | POST | 分页查询调用记录 |

`/execute` 是核心入口。请求体是原始 JSON 字符串，由 Service 内部解析。

### 6.2 `/api/actuator/execute` 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tenant` | string | 是 | 租户，用于查询接口配置 |
| `apiCode` | string | 是 | 接口编码 |
| `token` | string | 是 | 授权 token |
| `systemCode` | string | 是 | 调用方系统编码 |
| `params` | object | 按接口配置 | 接口动态参数 |
| `fieldAuth` | boolean | 否 | 字段鉴权开关 |
| `pageNeed` | boolean | 否 | 是否强制分页 |
| `pageNum` | integer | 分页时必填 | 当前页码，从 1 开始 |
| `pageSize` | integer | 分页时必填 | 每页条数 |
| `exportExcel` | boolean | 否 | 是否导出 Excel |

### 6.3 Controller 执行流程

入口方法：`ApiActuatorController.execute(...)`

流程：

1. 记录开始时间。
2. 解析请求体中的 `exportExcel`。
3. 如果 `exportExcel=true`，调用 `apiActuatorService.executeExcel(...)`。
4. 否则调用 `apiActuatorService.execute(...)`。
5. 捕获异常：
   - `OpenException`：返回可展示错误。
   - 其他异常：返回隐藏详细信息的错误，提示可能存在 SQL 注入风险或执行异常。
6. 无论成功失败，都会调用 `insertAfterExecute(...)` 写执行记录。
7. 成功时：
   - 普通查询返回 `ResultModel.success(data)`。
   - Excel 导出写入响应流并返回 `null`。

### 6.4 Service 执行主流程

入口方法：`ApiActuatorServiceImpl.execute(String apiActuatorInfo, HttpServletRequest req)`

主要流程：

1. 解析请求 JSON：
   - `tenant`
   - `apiCode`
   - `fieldAuth`
   - `pageNeed`
   - `params`
2. 获取调用方 IP。
3. 校验 `apiCode` 不为空。
4. 调用 `apiService.getByApi(tenant, apiCode)` 查询接口配置。
5. 调用 `infoCheck(...)` 做权限、分页和入参校验，返回最终参数 `paramsMap`。
6. 调用 `concatSql(apiInterfaceVO, paramsMap)` 拼接 SQL。
7. 按接口配置的数据源切换执行环境：
   - 默认数据源：`tenantService.clearDs()`。
   - 非默认数据源：`tenantService.setDs(connection.code + "_master")`。
8. 根据分页规则执行：
   - 分页：`dbModule.page2(sql, pageNum, pageSize, paramsMap, true, fieldBackMode)`。
   - 不分页：`dbModule.select(sql, paramsMap, true, fieldBackMode)`。
9. `finally` 中清理数据源上下文。
10. 返回 SQL 执行结果。

分页判断逻辑：

```text
if pageNeed == true:
    分页
else if pageNeed == null and 接口配置 PAGE == 1:
    分页
else:
    不分页
```

## 7. 执行前校验逻辑

入口方法：`ApiActuatorServiceImpl.infoCheck(...)`

### 7.1 分页校验

如果请求需要分页：

1. `pageNum` 和 `pageSize` 必须传。
2. `pageNum` 和 `pageSize` 必须大于 0。
3. `pageSize` 不能超过配置项 `common.sql_result_page_max_row`。

配置位置：

```yaml
common:
  sql_result_page_max_row: 5000
```

### 7.2 权限校验

执行权限由 `TenantAuthServiceImpl.hasExecutePermission(apiId, token, ip)` 完成。

校验顺序：

1. token 不能为空。
2. apiId 不能为空。
3. 根据 token 查询 `API_SQL_TENANT_AUTH`。
4. 如果授权分组配置了 `IP_CONFIG`，则调用 IP 必须在白名单中。
5. 查询 `API_SQL_TENANT_INTERFACE`，确认该授权分组关联了当前接口。

只有以上全部通过，才允许继续执行。

### 7.3 系统编码校验

`systemCode` 不能为空。该字段会写入执行记录，用于标识调用方系统。

### 7.4 参数存在性校验

参数配置来自 `API_SQL_INTERFACE_PARAM`。

规则：

1. 如果接口存在必填参数，但请求未传 `params`，报错。
2. 如果请求传了未知参数，报错。
3. 必填参数必须全部传入。
4. 非必填且配置了默认值的参数，如果调用方未传，则自动补入默认值。
5. 三个特殊鉴权参数不按普通参数处理：
   - `managerField`
   - `fundIdsField`
   - `fundCodesField`

### 7.5 参数类型校验

入口方法：`ApiActuatorServiceImpl.paramListValidate(...)`

| 类型值 | 类型 | 校验规则 |
| --- | --- | --- |
| `1` | 字符串 | 必须是 `String` |
| `2` | 整型 | 支持 `Integer`、`Long`、可转 Long 的字符串，并转成 `Long` |
| `3` | 浮点型 | 支持 `Float`、`Double`、`BigDecimal`、可转 Double 的字符串 |
| `4` | 日期型 | 必须是字符串，格式为 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `5` | 数组 | 必须是 `List`，不能为空，最终用分号拼接成字符串 |

### 7.6 正则校验

当 `VALIDATE_TYPE = 3` 时：

1. 使用 `EXPRESSION` 作为正则表达式。
2. 对参数值执行匹配。
3. 匹配失败时返回 `ERROR`。
4. `ERROR` 中可以使用 `#{param}` 引用其他参数值。

### 7.7 表达式校验

当 `VALIDATE_TYPE = 2` 时：

1. `EXPRESSION` 被当成 SQL 表达式执行。
2. 表达式中的 `#{param}` 从当前参数集中取值。
3. 会切换到接口配置的数据源执行 `dbModule.selectInt(...)`。
4. 如果查询结果小于 1，则校验失败并返回 `ERROR`。

## 8. 字段鉴权逻辑

入口方法：`ApiActuatorServiceImpl.managerProductCheck(...)`

支持三个特殊参数：

| 参数名 | 说明 | 对应接口配置字段 |
| --- | --- | --- |
| `managerField` | 管理人 ID | `MANAGER_FIELD` |
| `fundIdsField` | 产品 ID 列表 | `FUND_IDS_FIELD` |
| `fundCodesField` | 产品代码列表 | `FUND_CODES_FIELD` |

### 8.1 `fieldAuth=true`

此时鉴权主动权在接口配置端。

规则：

1. 如果接口配置了 `MANAGER_FIELD`，请求必须传 `managerField`。
2. 如果只配置了 `FUND_IDS_FIELD`，请求必须传 `fundIdsField`。
3. 如果只配置了 `FUND_CODES_FIELD`，请求必须传 `fundCodesField`。
4. 如果同时配置了产品 ID 和产品代码字段，则 `fundIdsField` 与 `fundCodesField` 二选一即可；如果两者都传，则两个条件都会参与 SQL。

### 8.2 `fieldAuth=false` 或未传

此时鉴权主动权在接口调用端。

规则：

1. 调用方不传特殊鉴权参数，则不拼对应鉴权条件。
2. 调用方传了特殊鉴权参数，则接口配置中必须存在对应字段。
3. 传入值必须符合类型要求。

## 9. 实际 SQL 拼接逻辑

入口方法：`ApiActuatorServiceImpl.concatSql(...)`

当前执行 SQL 的拼接顺序：

```text
SELECT selectParam
FROM fromParam
WHERE ( 1 = 1 )
[AND 或直接拼接 whereParamFixed]
[AND managerField = ...]
[AND @IN{fundIdsField, fundIdsFieldColumn}]
[AND @IN{fundCodesField, fundCodesFieldColumn}]
[GROUP BY groupParam]
[ORDER BY orderParam]
```

固定 WHERE 条件处理：

1. 如果 `whereParamFixed` 以 `?AND` 或 `?OR` 开头，则直接拼接：

```text
WHERE ( 1 = 1 ) (?AND{...})
```

2. 否则加 `AND (...)`：

```text
WHERE ( 1 = 1 ) AND (whereParamFixed)
```

注意事项：

1. `whereParamChange` 当前在实际执行中未启用。
2. SQL 片段本身来自接口配置，系统不会对 `selectParam`、`fromParam`、`whereParamFixed` 等做结构化 SQL AST 校验。
3. 参数占位符最终由 `BoundSql` 和 `TextSqlNode` 解析。

## 10. SQL 占位符解析规则

入口：`BoundSql` 初始化时调用 `TextSqlNode.parseSql(...)`。

### 10.1 `#{param}`

参数绑定语法。

示例：

```sql
T.STATUS = #{status}
```

解析后：

```sql
T.STATUS = ?
```

同时将 `paramsMap.status` 加入 JDBC 参数列表。

如果值是集合或数组，会展开成多个 `?`。

### 10.2 `${param}`

字符串直接拼接语法。

示例：

```sql
ORDER BY ${orderField}
```

会直接把参数值拼进 SQL。该方式存在 SQL 注入风险，应谨慎使用，优先使用 `#{}`。

### 10.3 `?{param, sql}`

条件片段语法。

示例：

```sql
?{status, T.STATUS = #{status}}
```

如果 `status` 有值，则保留后面的 SQL；否则整个片段清空。

### 10.4 `?AND{param, sql}` 和 `?OR{param, sql}`

带连接符的条件片段。

示例：

```sql
?AND{status, T.STATUS = #{status}}
?OR{name, T.NAME = #{name}}
```

如果参数有值，分别自动补 `AND` 或 `OR`。

### 10.5 `@IN{param, column}`

IN 条件语法，主要用于列表参数。

示例：

```sql
@IN{fundIdsField, T.FUND_ID}
```

如果 `fundIdsField` 值是：

```text
1;2;3
```

则解析为：

```sql
(T.FUND_ID IN (?, ?, ?))
```

同时为了兼容 Oracle，超过 1000 个值时会自动拆成多个 IN 条件。

### 10.6 `@NIN{param, column}`

NOT IN 条件语法，与 `@IN{}` 类似。

### 10.7 `@LIKE{}`、`@LIKER{}`、`@LIKEL{}`

模糊匹配语法：

| 语法 | 含义 |
| --- | --- |
| `@LIKE{param, column}` | `%value%` |
| `@LIKER{param, column}` | `value%` |
| `@LIKEL{param, column}` | `%value` |

## 11. DbModule 执行逻辑

`DbModule` 是最终执行 SQL 的统一入口。

### 11.1 普通查询

入口方法：`DbModule.select(sql, params, saveSql, fieldBackMode)`

流程：

1. 创建 `BoundSql`。
2. `BoundSql` 解析 SQL 占位符，形成最终 SQL 和 JDBC 参数数组。
3. `DbModule.execute(...)` 根据 `tenantService.getDs()` 切换数据源。
4. 执行 SQL 拦截器 `SQLInterceptor.preHandle(...)`。
5. 先执行 count SQL，记录结果数量到 `SqlContextHolder`。
6. 再执行原 SQL，返回 `List<Map<String, Object>>`。
7. 根据 `fieldBackMode` 决定字段名返回模式。

### 11.2 分页查询

入口方法：`DbModule.page2(sql, page, limit, params, saveSql, fieldBackMode)`

流程：

1. 创建 `BoundSql`。
2. 自动识别数据库方言。
3. 生成 count SQL，查询总数。
4. 根据方言生成分页 SQL。
5. 查询当前页数据。
6. 返回 `PageInfoResult`。

### 11.3 字段返回模式

常量定义：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `API_FIELD_BACK_MODE_DEFAULT` | `1` | 默认模式，字段名转小写 |
| `API_FIELD_BACK_MODE_ALIAS_NAME` | `2` | 按 SQL 返回别名原样返回 |

执行查询时，如果配置为 `API_FIELD_BACK_MODE_ALIAS_NAME`，使用 `OriginalCaseColumnMapRowMapper`；否则使用默认列名映射器。

### 11.4 SQL 和结果数量记录

当 `saveSql=true` 时：

1. `TextSqlNode` 会将参数值替换回 SQL 文本。
2. 最终 SQL 保存到 `SqlContextHolder`。
3. count 查询结果保存到 `SqlContextHolder`。
4. `insertAfterExecute(...)` 会将这些值写入 `API_SQL_ACTUATOR`。

## 12. 动态数据源逻辑

### 12.1 数据源上下文

项目内有两层上下文：

| 类 | 作用 |
| --- | --- |
| `DsContextHolder` | 保存业务侧当前数据源 key |
| `DynamicDataSourceContextHolder` | 动态数据源组件实际读取的数据源栈 |

执行接口时：

1. `ApiActuatorServiceImpl` 调用 `tenantService.setDs(...)` 设置业务数据源。
2. `DbModule.execute(...)` 读取 `tenantService.getDs()`。
3. 如果不为空，将数据源 key push 到 `DynamicDataSourceContextHolder`。
4. `DynamicRoutingDataSource.determineDataSource()` 根据当前 key 找到实际 `DataSource`。
5. 执行结束后 poll 清理上下文。

### 12.2 数据源初始化和同步

入口方法：

| 方法 | 说明 |
| --- | --- |
| `ConnectionServiceImpl.initialize()` | 初始化全部 `db` 类型连接 |
| `ConnectionServiceImpl.sync()` | 同步当前租户下的数据源配置 |
| `DataSourceServiceImpl.addDataSource(...)` | 添加动态数据源 |
| `DataSourceServiceImpl.deleteDataSource(...)` | 删除动态数据源 |

动态数据源 key 的实际格式：

```text
connection.code + "_master"
```

## 13. Excel 导出执行逻辑

导出入口仍是：

```text
POST /api/actuator/execute
```

请求中传：

```json
{
  "exportExcel": true
}
```

执行流程：

1. Controller 判断 `exportExcel=true`。
2. 调用 `ApiActuatorServiceImpl.executeExcel(...)`。
3. `executeExcel(...)` 内部复用 `execute(...)`，确保校验、SQL 拼接、分页和数据源切换与普通查询一致。
4. 将查询结果交给 `ExcelExportUtils.toExcelBytes(...)` 转为 Excel。
5. 文件名格式：

```text
apiCode_yyyyMMddHHmmss.xlsx
```

6. Controller 写响应头：

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment;filename=...
Access-Control-Expose-Headers: Content-Disposition
```

导出范围：

1. 如果请求分页，则导出当前页。
2. 如果接口配置分页且 `pageNeed` 不传，则导出当前页。
3. 如果不分页，则导出本次 SQL 返回的全部数据。

## 14. 执行记录查询逻辑

入口：

```text
POST /api/actuator/page
```

实现方法：`ApiActuatorServiceImpl.page(ApiActuatorDTO dto)`

支持条件：

| 条件 | 匹配方式 |
| --- | --- |
| 当前租户 | 精确匹配 |
| `apiId` | 精确匹配 |
| `apiCode` | 不区分大小写模糊匹配 |
| `systemCode` | 不区分大小写模糊匹配 |
| `interIp` | 精确匹配 |
| `executeResult` | 精确匹配 |
| `executeBy` | 精确匹配 |
| `executeTimeStart` | 按日期大于等于 |
| `executeTimeEnd` | 按日期小于等于 |
| `interParam` | 模糊匹配 |

排序：

```text
id desc
```

## 15. 典型调用示例

### 15.1 普通分页查询

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

### 15.2 不分页查询

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "pageNeed": false,
  "params": {
    "status": "1"
  }
}
```

### 15.3 Excel 导出

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

### 15.4 字段鉴权调用

```json
{
  "tenant": "demo",
  "apiCode": "demoApi",
  "token": "调用token",
  "systemCode": "front-system",
  "fieldAuth": true,
  "params": {
    "managerField": 525,
    "fundIdsField": [1, 2, 3]
  }
}
```

## 16. 需要重点关注的实现细节

### 16.1 预览 SQL 与执行 SQL 不完全一致

`ApiServiceImpl.preview(...)` 会拼接 `whereParamChange`，但 `ApiActuatorServiceImpl.concatSql(...)` 中 `whereParamChange` 相关代码被注释。因此页面预览 SQL 可能包含可变 WHERE，而实际执行 SQL 不包含。

### 16.2 管理人字段拼接建议复核

`ApiActuatorServiceImpl.concatSql(...)` 中管理人字段目前通过字符串方式拼接：

```java
sql = sql + " AND " + apiInterface.getManagerField() + " = '" + paramsMap.get(API_PARAM_MANAGER_ID);
```

该逻辑存在两个需要复核的点：

1. 未使用 `#{managerField}` 参数绑定。
2. 字符串右侧引号看起来没有闭合。

虽然 `managerField` 在参数校验中会校验为整型，但这里仍建议改成参数绑定方式，保持与其他动态参数一致。

### 16.3 `${}` 直接拼接存在注入风险

`TextSqlNode` 支持 `${param}` 直接把值拼进 SQL。除非确实需要拼接表名、字段名或排序片段，否则建议优先使用 `#{param}`。

### 16.4 接口配置片段缺少结构化 SQL 校验

`selectParam`、`fromParam`、`whereParamFixed`、`groupParam`、`orderParam` 都是配置文本。执行前主要依靠 SQL 拦截器、数据库执行异常和调用方权限控制。配置人员需要确保 SQL 片段可信且正确。

### 16.5 `ApiPreHandle` 本项目内未见实现

Controller 上标注了 `ApiPreHandle(minAccessRole = "...")`，但本项目内没有看到对应 AOP 或拦截器。租户、用户、角色等上下文来源需要结合外部公共组件或部署环境确认。

## 17. 一句话流程图

```text
/api/interface/save
  -> 校验接口配置和参数
  -> 生成 previewSql
  -> 保存 API_SQL_INTERFACE
  -> 保存 API_SQL_INTERFACE_PARAM

/api/actuator/execute
  -> 解析 tenant/apiCode/token/params
  -> 查询 API_SQL_INTERFACE + API_SQL_INTERFACE_PARAM
  -> 校验 token、IP、授权关系
  -> 校验分页和参数
  -> 拼接 SQL
  -> 切换数据源
  -> BoundSql 解析占位符
  -> DbModule 执行 count 和查询
  -> 返回 JSON 或 Excel
  -> 写入 API_SQL_ACTUATOR
```

