package com.citics.glxtapi.plugin.sql;

import cn.hutool.core.util.StrUtil;
import com.citics.glxtapi.common.page.PageInfoResult;
import com.citics.glxtapi.common.utils.page.PageHelperUtils;
import com.citics.glxtapi.common.utils.page.PageUtils;
import com.citics.glxtapi.plugin.db.DynamicRoutingDataSource;
import com.citics.glxtapi.plugin.db.support.DynamicDataSourceContextHolder;
import com.citics.glxtapi.plugin.sql.dialect.Dialect;
import com.citics.glxtapi.plugin.sql.dialect.DialectAdapter;
import com.citics.glxtapi.plugin.sql.interceptor.SQLInterceptor;
import com.citics.glxtapi.plugin.sql.table.NamedTable;
import com.citics.glxtapi.plugin.sql.table.SQLTable;
import com.citics.glxtapi.web.context.RequestContext;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.interceptor.ResultProvider;
import com.citics.glxtapi.web.entity.vo.ProcedureExecuteResult;
import com.citics.glxtapi.web.exception.APIException;
import com.citics.glxtapi.web.model.Page;
import com.citics.glxtapi.web.service.TenantService;
import com.citics.glxtapi.web.support.SqlContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.beans.Transient;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.citics.glxtapi.web.constants.Constants.*;

@Slf4j
public class DbModule implements ModuleService {

    private final DynamicRoutingDataSource dynamicRoutingDataSource;
    private final TenantService tenantService;
    protected JdbcTemplate jdbcTemplate;

    /**
     * 列名转换适配器
     */
    protected ColumnMapperAdapter columnMapperAdapter;

    private RowMapper<Map<String, Object>> columnMapRowMapper;
    private Function<String, String> rowMapColumnMapper;
    protected List<SQLInterceptor> sqlInterceptors;
    protected DialectAdapter dialectAdapter;

    /**
     * 结果提供者
     */
    private ResultProvider resultProvider;

    /**
     * 逻辑删除列名
     */
    private String logicDeleteColumn;

    /**
     * 逻辑删除列值
     */
    private String logicDeleteValue;

    public DbModule(JdbcTemplate jdbcTemplate, DynamicRoutingDataSource dynamicRoutingDataSource, TenantService tenantService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
        this.tenantService = tenantService;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transient
    public void setResultProvider(ResultProvider resultProvider) {
        this.resultProvider = resultProvider;
    }

    @Transient
    public void setColumnMapRowMapper(RowMapper<Map<String, Object>> columnMapRowMapper) {
        this.columnMapRowMapper = columnMapRowMapper;
    }

    @Transient
    public void setColumnMapperProvider(ColumnMapperAdapter columnMapperAdapter) {
        this.columnMapperAdapter = columnMapperAdapter;
    }

    @Transient
    public void setRowMapColumnMapper(Function<String, String> rowMapColumnMapper) {
        this.rowMapColumnMapper = rowMapColumnMapper;
    }

    @Transient
    public void setSqlInterceptors(List<SQLInterceptor> sqlInterceptors) {
        this.sqlInterceptors = sqlInterceptors;
    }

    public void setDialectAdapter(DialectAdapter dialectAdapter) {
        this.dialectAdapter = dialectAdapter;
    }

    @Transient
    public String getLogicDeleteColumn() {
        return logicDeleteColumn;
    }

    @Transient
    public void setLogicDeleteColumn(String logicDeleteColumn) {
        this.logicDeleteColumn = logicDeleteColumn;
    }

    @Transient
    public String getLogicDeleteValue() {
        return logicDeleteValue;
    }

    @Transient
    public void setLogicDeleteValue(String logicDeleteValue) {
        this.logicDeleteValue = logicDeleteValue;
    }

    /**
     * 开启事务，在一个回调中进行操作
     *
     * @param function 回调函数，如：()=>{......}
     */
    public Object transaction(Function<DbModule, Object> function) {
        // 创建事务
        Transaction transaction = transaction();
        try {
            Object val = function.apply(this);
            transaction.commit();
            //提交事务
            return val;
        } catch (Throwable throwable) {
            log.error("transaction error", throwable);
            transaction.rollback();     //回滚事务
            throw throwable;
        }
    }

    /**
     * 开启事务，手动提交和回滚
     *
     * @return
     */
    public Transaction transaction() {
        try {
            String ds = this.tenantService.getDs();
            if (StrUtil.isNotEmpty(ds)) {
                DynamicDataSourceContextHolder.push(ds);
            }
            return new Transaction(new DataSourceTransactionManager(Objects.requireNonNull(this.jdbcTemplate.getDataSource())));
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * 查询SQL，返回List类型结果
     *
     * @param sql SQL语句
     */
    public List<Map<String, Object>> select(String sql, boolean saveSql, Integer fieldBackMode) {
        return select(sql, null, saveSql, fieldBackMode);
    }

    /**
     * 查询SQL，并传入变量信息，返回List类型结果
     *
     * @param sql    SQL语句
     * @param params 变量信息
     * @return
     */
    public List<Map<String, Object>> select(String sql, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        return select(new BoundSql(sql, params, this, saveSql, fieldBackMode));
    }

    @Transient
    public List<Map<String, Object>> select(BoundSql boundSql) {
        return queryForList(boundSql);
    }

    private List<Map<String, Object>> queryForList(BoundSql boundSql) {
        // 1. 交给execute统一执行（切换数据源、执行拦截器）
        return this.execute(boundSql, () -> {
            // 2. 再进入 BoundSql 的 execute（内部做SQL执行监控、日志等）
            return boundSql.execute(() -> {
                // 3. 获取数据库方言（MySQL、Oracle、达梦...）
                Dialect dialect = this.getDialect();
                // 4. 生成 count SQL（用于分页总数）
                BoundSql countBoundSql = boundSql.copy(dialect.getCountSql(boundSql.getSql()));
                // 5. 执行 count 查询，获取总条数
                Integer count = countBoundSql.execute(() -> this.jdbcTemplate.query(
                        countBoundSql.getSql(),
                        new SingleRowResultSetExtractor<>(Integer.class),
                        countBoundSql.getParameters()
                ));

                // 6. 把总数存入上下文，供上层分页工具使用
                SqlContextHolder.clearCount();
                SqlContextHolder.setSqlCount(count);

                // 7. 真正执行 SELECT 查询
                Integer fieldBackMode = boundSql.getFieldBackMode();
                List<Map<String, Object>> list = this.jdbcTemplate.query(
                        boundSql.getSql(),
                        (fieldBackMode != null && fieldBackMode.equals(API_FIELD_BACK_MODE_ALIAS_NAME))
                                ? new OriginalCaseColumnMapRowMapper() : this.columnMapRowMapper,
                        boundSql.getParameters()
                );

                // 8. 如果配置了排除字段，删掉这些字段
                if (boundSql.getExcludeColumns() != null) {
                    list.forEach(row -> boundSql.getExcludeColumns().forEach(row::remove));
                }

                // 9. 返回最终列表
                return list;
            });
        });
    }

    public List<Map<String, Object>> queryList(String sql) {
        BoundSql boundSql = new BoundSql(sql, null, this, true, null);
        return this.execute(boundSql, () -> this.jdbcTemplate.query(sql, this.columnMapRowMapper, null));
    }

    /**
     * 执行update操作，返回受影响行数
     *
     * @param sql SQL语句
     * @return
     */
    public int update(String sql) {
        return update(sql, null);
    }

    /**
     * 执行update操作，并传入变量信息，返回受影响行数
     *
     * @param sql    SQL语句
     * @param params
     * @return
     */
    public int update(String sql, Map<String, Object> params) {
        return update(new BoundSql(sql, params, this, false, null));
    }

    @Transient
    public int update(BoundSql boundSql) {
        return this.execute(boundSql, () -> {
            Object value = this.jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters());
            return (int) value;
        });
    }

    /**
     * 执行insert操作，返回插入主键
     *
     * @param sql SQL语句
     * @return
     */
    public Object insert(String sql) {
        return insert(sql, null, null);
    }

    /**
     * 执行insert操作，并传入变量信息，返回插入主键
     *
     * @param sql    SQL语句
     * @param params 变量信息
     * @return
     */
    public Object insert(String sql, Map<String, Object> params) {
        return insert(sql, null, params);
    }

    /**
     * 执行insert操作，返回插入主键
     *
     * @param sql     SQL语句
     * @param primary 主键列
     * @return
     */
    public Object insert(String sql, String primary) {
        return insert(sql, primary, null);
    }

    /**
     * 执行insert操作，并传入主键和变量信息，返回插入主键
     *
     * @param sql     SQL语句
     * @param primary 主键列
     * @param params  变量信息
     * @return
     */
    public Object insert(String sql, String primary, Map<String, Object> params) {
        return insert(new BoundSql(sql, params, this, false, null), primary);
    }

    @Transient
    public Object insert(BoundSql boundSql, String primary) {
        return this.execute(boundSql, () -> {
            KeyHolder keyHolder = new KeyHolder(primary);
            this.jdbcTemplate.update(con -> {
                PreparedStatement ps = keyHolder.createPreparedStatement(con, boundSql.getSql());
                new ArgumentPreparedStatementSetter(boundSql.getParameters()).setValues(ps);
                return ps;
            }, keyHolder);
            Object value = keyHolder.getObjectKey();
            return value;
        });
    }

    /**
     * 批量执行操作，返回受影响的行数
     *
     * @param sql  SQL语句
     * @param args 参数
     * @return
     */
    public int batchUpdate(String sql, List<Object[]> args) {
        return this.execute(null, () -> {
            int[] values = this.jdbcTemplate.batchUpdate(sql, args);
            return Arrays.stream(values).sum();
        });
    }

    /**
     * 批量执行操作，返回受影响的行数
     *
     * @param sql        SQL语句
     * @param batchSize  批量插入每次数量
     * @param args
     * @return
     */
    public int batchUpdate(String sql, int batchSize, List<Object[]> args) {
        return this.execute(null, () -> {
            int[][] values = this.jdbcTemplate.batchUpdate(sql, args, batchSize, (ps, arguments) -> {
                int colIndex = 1;
                for (Object value : arguments) {
                    if (value instanceof SqlParameterValue) {
                        SqlParameterValue paramValue = (SqlParameterValue) value;
                        StatementCreatorUtils.setParameterValue(ps, colIndex++, paramValue, paramValue.getValue());
                    } else {
                        StatementCreatorUtils.setParameterValue(ps, colIndex++,
                                StatementCreatorUtils.javaTypeToSqlParameterType(value == null ? null : value.getClass()), value);
                    }
                }
                colIndex = 1;
            });
            int count = 0;
            for (int[] value : values) {
                count += Arrays.stream(value).sum();
            }
            return count;
        });
    }

    /**
     * 批量执行操作，返回受影响的行数
     *
     * @param sqls SQL语句
     * @return
     */
    public int batchUpdate(List<String> sqls) {
        return this.execute(null, () -> {
            int[] values = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[0]));
            return Arrays.stream(values).sum();
        });
    }

    /**
     * 执行分页查询，分页条件手动传入
     *
     * @param sql    SQL语句
     * @param limit  限制条数
     * @param offset 跳过条数
     * @return
     */
    public Object page(String sql, long limit, long offset, boolean saveSql, Integer fieldBackMode) {
        return page(sql, limit, offset, null, saveSql, fieldBackMode);
    }

    /**
     * 执行分页查询，并传入变量信息，分页条件手动传入
     *
     * @param sql    SQL语句
     * @param limit  限制条数
     * @param offset 跳过条数
     * @param params 变量信息
     * @return
     */
    public Object page(String sql, long limit, long offset, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        BoundSql boundSql = new BoundSql(sql, params, this, saveSql, fieldBackMode);
        return page(boundSql, new Page(limit, offset));
    }

    /**
     * 执行分页查询
     *
     * @param sql        SQL语句
     * @param page       当前页
     * @param limit      限制条数
     * @param saveSql    是否将最终执行的sql保存至执行器
     * @param fieldBackMode
     * @return
     */
    public Object page1(String sql, long page, long limit, boolean saveSql, Integer fieldBackMode) {
        return page1(sql, page, limit, null, saveSql, fieldBackMode);
    }

    /**
     * 执行分页查询，并传入变量信息，分页条件手动传入
     *
     * @param sql    SQL语句
     * @param page   当前页
     * @param limit  限制条数
     * @param params 变量信息
     * @return
     */
    public Object page1(String sql, long page, long limit, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        BoundSql boundSql = new BoundSql(sql, params, this, saveSql, fieldBackMode);
        return page(boundSql, Page.createPage(page, limit));
    }

    /**
     * 执行分页查询
     *
     * @param sql        SQL语句
     * @param page       当前页
     * @param limit      限制条数
     * @param saveSql    是否将最终执行的sql保存至执行器
     * @param fieldBackMode
     * @return
     */
    public Object page2(String sql, long page, long limit, boolean saveSql, Integer fieldBackMode) {
        return page2(sql, page, limit, null, saveSql, fieldBackMode);
    }

    /**
     * 执行分页查询，并传入变量信息，分页条件手动传入
     *
     * @param sql    SQL语句
     * @param page   当前页
     * @param limit  限制条数
     * @param params 变量信息
     * @return
     */
    public Object page2(String sql, long page, long limit, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        BoundSql boundSql = new BoundSql(sql, params, this, saveSql, fieldBackMode);
        return queryForPage(boundSql, page, limit);
    }

    private PageInfoResult queryForPage(BoundSql boundSql, long page, long limit) {
        return this.execute(boundSql, () -> boundSql.execute(() -> {
            Dialect dialect = this.getDialect();
            BoundSql countBoundSql = boundSql.copy(dialect.getCountSql(boundSql.getSql()));
            Integer count = countBoundSql.execute(() -> this.jdbcTemplate.query(
                    countBoundSql.getSql(),
                    new SingleRowResultSetExtractor<>(Integer.class),
                    countBoundSql.getParameters()
            ));

            SqlContextHolder.clearCount();
            SqlContextHolder.setSqlCount(count);

            // 多数据源有问题，pageHelper需要自动切换数据源才行
            BoundSql pageBoundSql = buildPageBoundSql(dialect, boundSql, (page - 1) * limit, limit);
            List<Map<String, Object>> list = pageBoundSql.execute(() -> {
                Integer fieldBackMode = boundSql.getFieldBackMode();
                List<Map<String, Object>> result = this.jdbcTemplate.query(
                        pageBoundSql.getSql(),
                        (fieldBackMode != null && fieldBackMode.equals(API_FIELD_BACK_MODE_ALIAS_NAME))
                                ? new OriginalCaseColumnMapRowMapper() : this.columnMapRowMapper,
                        pageBoundSql.getParameters()
                );
                if (pageBoundSql.getExcludeColumns() != null) {
                    result.forEach(row -> pageBoundSql.getExcludeColumns().forEach(row::remove));
                }
                return result;
            });

            return PageHelperUtils.list2PageInfo(list, (int) page, (int) limit, count);
        }));
    }

    @Transient
    public Object page(BoundSql boundSql, Page page) {
        return this.execute(boundSql, () -> {
            Dialect dialect = this.getDialect();
            BoundSql countBoundSql = boundSql.copy(dialect.getCountSql(boundSql.getSql()));
            int count = countBoundSql.execute(() -> this.jdbcTemplate.query(
                    countBoundSql.getSql(),
                    new SingleRowResultSetExtractor<>(Integer.class),
                    countBoundSql.getParameters()
            ));

            List<Map<String, Object>> list = null;
            if (count > 0) {
                BoundSql pageBoundSql = buildPageBoundSql(dialect, boundSql, page.getOffset(), page.getLimit());
                list = pageBoundSql.execute(() -> {
                    List<Map<String, Object>> result = this.jdbcTemplate.query(
                            pageBoundSql.getSql(),
                            this.columnMapRowMapper,
                            pageBoundSql.getParameters()
                    );
                    if (pageBoundSql.getExcludeColumns() != null) {
                        result.forEach(row -> pageBoundSql.getExcludeColumns().forEach(row::remove));
                    }
                    return result;
                });
            }
            return PageUtils.getPageResult(page.getPage(), page.getLimit(), count, list);
        });
    }

    private BoundSql buildPageBoundSql(Dialect dialect, BoundSql boundSql, long offset, long limit) {
        String pageSql = dialect.getPageSql(boundSql.getSql(), boundSql, offset, limit);
        return boundSql.copy(pageSql);
    }

    /**
     * 查询int值，适合单行单列int的结果
     *
     * @param sql     SQL语句
     * @param saveSql
     * @param fieldBackMode
     * @return
     */
    public Integer selectInt(String sql, boolean saveSql, Integer fieldBackMode) {
        return selectInt(sql, null, saveSql, fieldBackMode);
    }

    /**
     * 查询int值，并传入变量信息，适合单行单列int的结果
     *
     * @param sql     SQL语句
     * @param params  变量信息
     * @param saveSql
     * @param fieldBackMode
     * @return
     */
    public Integer selectInt(String sql, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        return selectInt(new BoundSql(sql, params, this, saveSql, fieldBackMode));
    }

    @Transient
    public Integer selectInt(BoundSql boundSql) {
        return this.execute(boundSql, () -> boundSql.execute(() -> this.jdbcTemplate.query(boundSql.getSql(),
                new SingleRowResultSetExtractor<>(Integer.class), boundSql.getParameters())));
    }

    /**
     * 查询单行单列的值
     *
     * @param sql sql语句
     * @return
     */
    public Object selectValue(String sql, boolean saveSql, Integer fieldBackMode) {
        return selectValue(sql, null, saveSql, fieldBackMode);
    }

    /**
     * 查询单行单列的值，并传入变量信息
     *
     * @param sql    sql语句
     * @param params 变量信息
     * @return
     */
    public Object selectValue(String sql, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        BoundSql boundSql = new BoundSql(sql, params, this, saveSql, fieldBackMode);
        return this.execute(boundSql, () -> boundSql.execute(() -> this.jdbcTemplate.query(
                boundSql.getSql(),
                new SingleRowResultSetExtractor<>(Object.class),
                boundSql.getParameters()
        )));
    }

    public ProcedureExecuteResult callProcedure(String procedureName, List<ApiParam> paramList,
                                                Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        List<ApiParam> orderedParams = new ArrayList<>(paramList == null ? Collections.emptyList() : paramList);
        // CallableStatement按位置绑定参数，不能依赖参数名；ORDER_NO必须与存储过程签名顺序一致。
        orderedParams.sort(Comparator.comparing(x -> x.getOrderNo() == null ? Integer.MAX_VALUE : x.getOrderNo()));
        String callSql = buildProcedureCallSql(procedureName, orderedParams.size());
        if (saveSql) {
            // 先记录调用文本，保证过程执行失败时调用日志也能看到具体的{ call ... }。
            SqlContextHolder.clear();
            SqlContextHolder.setSql(callSql);
            SqlContextHolder.clearCount();
            SqlContextHolder.setSqlCount(0);
        }
        return this.execute(null, () -> this.jdbcTemplate.execute((ConnectionCallback<ProcedureExecuteResult>) connection -> {
            ProcedureExecuteResult result = new ProcedureExecuteResult();
            try (CallableStatement callableStatement = connection.prepareCall(callSql)) {
                for (int i = 0; i < orderedParams.size(); i++) {
                    ApiParam param = orderedParams.get(i);
                    int index = i + 1;
                    int sqlType = getProcedureSqlType(param.getJdbcType());
                    // OUT/INOUT必须先注册输出类型，Oracle游标按Types.CURSOR(-10)处理。
                    if (PROCEDURE_PARAM_DIRECTION_OUT == param.getDirection()
                            || PROCEDURE_PARAM_DIRECTION_INOUT == param.getDirection()) {
                        callableStatement.registerOutParameter(index, sqlType);
                    }
                    // IN/INOUT再写入调用方传入或默认补齐后的参数值。
                    if (PROCEDURE_PARAM_DIRECTION_IN == param.getDirection()
                            || PROCEDURE_PARAM_DIRECTION_INOUT == param.getDirection()) {
                        Object value = params == null ? null : params.get(param.getCode());
                        if (value == null) {
                            // Oracle驱动对setObject(index, null)兼容性不稳定，空值必须按JDBC类型显式绑定。
                            callableStatement.setNull(index, sqlType);
                        } else if (sqlType == Types.CLOB) {
                            String text = (String) value;
                            callableStatement.setClob(index, new StringReader(text), (long) text.length());
                        } else {
                            callableStatement.setObject(index, value);
                        }
                    }
                }
                callableStatement.execute();
                int resultCount = 0;
                for (int i = 0; i < orderedParams.size(); i++) {
                    ApiParam param = orderedParams.get(i);
                    if (PROCEDURE_PARAM_DIRECTION_OUT != param.getDirection()
                            && PROCEDURE_PARAM_DIRECTION_INOUT != param.getDirection()) {
                        continue;
                    }
                    int index = i + 1;
                    if (isProcedureCursorType(param.getJdbcType())) {
                        Object value = callableStatement.getObject(index);
                        if (value instanceof ResultSet) {
                            ResultSet resultSet = (ResultSet) value;
                            try {
                                // 游标结果按参数code分组返回，便于多游标场景下前端分别渲染表格。
                                List<Map<String, Object>> rows = resultSetToList(resultSet, fieldBackMode);
                                result.getCursors().put(param.getCode(), rows);
                                resultCount += rows.size();
                            } finally {
                                resultSet.close();
                            }
                        } else {
                            result.getCursors().put(param.getCode(), Collections.emptyList());
                        }
                    } else {
                        result.getOutParams().put(param.getCode(),
                                getProcedureSqlType(param.getJdbcType()) == Types.CLOB
                                        ? readProcedureClob(callableStatement.getClob(index))
                                        : callableStatement.getObject(index));
                    }
                }
                result.setResultCount(resultCount);
                if (saveSql) {
                    // RESULT_COUNT记录所有游标行数合计；无游标或执行失败时默认记0。
                    SqlContextHolder.setSqlCount(resultCount);
                }
                return result;
            }
        }));
    }

    public static String buildProcedureCallSql(String procedureName, int paramCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("{ call ").append(procedureName).append("(");
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("?");
        }
        builder.append(") }");
        return builder.toString();
    }

    private String readProcedureClob(Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }
        try (Reader reader = clob.getCharacterStream()) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                text.append(buffer, 0, count);
            }
            return text.toString();
        } catch (IOException e) {
            throw new SQLException("读取存储过程CLOB输出失败", e);
        } finally {
            clob.free();
        }
    }

    private int getProcedureSqlType(String jdbcType) {
        if (StringUtils.isBlank(jdbcType)) {
            throw new APIException("存储过程JDBC类型不能为空");
        }
        String type = jdbcType.trim().toUpperCase(Locale.ROOT);
        if (PROCEDURE_JDBC_TYPE_CLOB.equals(type)) {
            return Types.CLOB;
        }
        if (PROCEDURE_JDBC_TYPE_VARCHAR.equals(type)) {
            return Types.VARCHAR;
        }
        if (PROCEDURE_JDBC_TYPE_INTEGER.equals(type)) {
            return Types.INTEGER;
        }
        if (PROCEDURE_JDBC_TYPE_BIGINT.equals(type)) {
            return Types.BIGINT;
        }
        if (PROCEDURE_JDBC_TYPE_DECIMAL.equals(type)) {
            return Types.DECIMAL;
        }
        if (PROCEDURE_JDBC_TYPE_DATE.equals(type)) {
            return Types.DATE;
        }
        if (PROCEDURE_JDBC_TYPE_TIMESTAMP.equals(type)) {
            return Types.TIMESTAMP;
        }
        if (PROCEDURE_JDBC_TYPE_CURSOR.equals(type)) {
            // java.sql.Types在JDK 8中没有标准CURSOR常量，OracleTypes.CURSOR实际值为-10。
            return -10;
        }
        throw new APIException("不支持的存储过程JDBC类型：" + jdbcType);
    }

    private boolean isProcedureCursorType(String jdbcType) {
        return jdbcType != null && PROCEDURE_JDBC_TYPE_CURSOR.equals(jdbcType.trim().toUpperCase(Locale.ROOT));
    }

    private List<Map<String, Object>> resultSetToList(ResultSet resultSet, Integer fieldBackMode) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                if (StringUtils.isBlank(columnName)) {
                    columnName = metaData.getColumnName(i);
                }
                // 与SQL查询保持一致：默认小写字段名；别名模式保留数据库返回的原始列名。
                if (fieldBackMode == null || !fieldBackMode.equals(API_FIELD_BACK_MODE_ALIAS_NAME)) {
                    columnName = columnName == null ? null : columnName.toLowerCase(Locale.ROOT);
                }
                row.put(columnName, resultSet.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 查询单条结果，查不到返回null
     *
     * @param sql sql语句
     * @return
     */
    public Map<String, Object> selectOne(String sql, boolean saveSql, Integer fieldBackMode) {
        return this.selectOne(sql, null, saveSql, fieldBackMode);
    }

    /**
     * 查询单条结果，并传入变量信息，查不到返回null
     *
     * @param sql    sql语句
     * @param params 参数
     * @return
     */
    public Map<String, Object> selectOne(String sql, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        return this.selectOne(new BoundSql(sql, params, this, saveSql, fieldBackMode));
    }

    /**
     * 查询单条结果
     *
     * @param boundSql boundSql
     * @return
     */
    @Transient
    public Map<String, Object> selectOne(BoundSql boundSql) {
        return this.execute(boundSql, () -> boundSql.execute(() -> {
            Map<String, Object> row = this.jdbcTemplate.query(boundSql.getSql(), new SingleRowResultSetExtractor<>(this.columnMapRowMapper),
                    boundSql.getParameters());
            if (row != null && boundSql.getExcludeColumns() != null) {
                boundSql.getExcludeColumns().forEach(row::remove);
            }
            return row;
        }));
    }

    /**
     * 指定table，进行单表操作
     *
     * @param tableName 表名
     * @return
     */
    public NamedTable table(String tableName) {
        return new NamedTable(tableName, this, rowMapColumnMapper);
    }

    public BoundDbModule ds(String dsKey) {
        return new BoundDbModule(dynamicRoutingDataSource, tenantService,
                sqlInterceptors, dialectAdapter,
                resultProvider, columnMapperAdapter,
                columnMapRowMapper, rowMapColumnMapper,
                logicDeleteColumn, logicDeleteValue,
                dsKey);
    }

    /**
     * 指定SQL，进行SQL相关操作
     *
     * @param sql 于SQL
     * @return
     */
    public SQLTable sql(String sql, boolean saveSql, Integer fieldBackMode) {
        return this.sql(sql, null, saveSql, fieldBackMode);
    }

    /**
     * 指定SQL，进行SQL相关操作
     *
     * @param sql    于SQL
     * @param params 参数
     * @return
     */
    public SQLTable sql(String sql, Map<String, Object> params, boolean saveSql, Integer fieldBackMode) {
        return new SQLTable(new BoundSql(sql, params, this, saveSql, fieldBackMode), this, rowMapColumnMapper);
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(BoundSql boundSql, Supplier<T> supplier) {
        try {
            // 1. 获取当前要使用的数据源（多租户用，比如切换到某个库）
            String ds = this.tenantService.getDs();
            if (StrUtil.isNotEmpty(ds)) {
                // 把数据源编号压入上下文，后面 jdbcTemplate 会自动用这个数据源
                DynamicDataSourceContextHolder.push(ds);
            }

            // 2. 如果有SQL，执行所有拦截器的预处理（比如权限、加条件、脱敏等）
            if (null != boundSql) {
                this.sqlInterceptors.forEach(interceptor -> interceptor.preHandle(boundSql, RequestContext.getRequestEntity()));
            }

            // 3. 真正执行业务逻辑
            Object result = supplier.get();

            // 4. 返回结果
            return (T) result;
        } finally {
            // 无论成功失败，都把刚才的数据源弹出，避免污染其他线程/请求
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * 复制新的
     *
     * @return
     */
    @Transient
    public DbModule cloneDBModule() {
        DbModule dbModule = new DbModule(this.jdbcTemplate, this.dynamicRoutingDataSource, this.tenantService);
        dbModule.setColumnMapperProvider(this.columnMapperAdapter);
        dbModule.setColumnMapRowMapper(this.columnMapRowMapper);
        dbModule.setRowMapColumnMapper(this.rowMapColumnMapper);
        dbModule.setSqlInterceptors(this.sqlInterceptors);
        dbModule.setDialectAdapter(this.dialectAdapter);
        dbModule.setResultProvider(this.resultProvider);
        dbModule.setLogicDeleteColumn(this.logicDeleteColumn);
        dbModule.setLogicDeleteValue(this.logicDeleteValue);
        return dbModule;
    }

    /**
     * 指定列名转换
     *
     * @param name
     * @return
     */
    public DbModule columnCase(String name) {
        DbModule dbModule = cloneDBModule();
        dbModule.setColumnMapRowMapper(this.columnMapperAdapter.getColumnMapRowMapper(name));
        dbModule.setRowMapColumnMapper(this.columnMapperAdapter.getRowMapColumnMapper(name));
        return dbModule;
    }

    @Transient
    public String getDataSourceName() {
        String ds = this.tenantService.getDs();
        return StrUtil.isNotEmpty(ds) ? ds : "默认数据源";
    }

    public Dialect getDialect() {
        DataSource dataSource = null;
        try {
            String ds = this.tenantService.getDs();
            if (StrUtil.isNotEmpty(ds)) {
                DynamicDataSourceContextHolder.push(ds);
            }
            dataSource = this.jdbcTemplate.getDataSource();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        if (null == dataSource) {
            throw new APIException("自动获取数据库方言失败,数据源获取结果为空");
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Dialect dialect = dialectAdapter.getDialectFromConnection(connection);
            if (dialect == null) {
                throw new APIException("自动获取数据库方言失败，方言获取结果为空");
            }
            return dialect;
        } catch (Exception e) {
            throw new APIException("自动获取数据库方言失败", e);
        } finally {
            if (null != connection) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * 采用驼峰列名
     *
     * @return
     */
    public DbModule camel() {
        return columnCase("camel");
    }

    /**
     * 采用帕斯卡列名
     *
     * @return
     */
    public DbModule pascal() {
        return columnCase("pascal");
    }

    /**
     * 采用全小写列名
     *
     * @return
     */
    public DbModule lower() {
        return columnCase("lower");
    }

    /**
     * 采用全大写列名
     *
     * @return
     */
    public DbModule upper() {
        return columnCase("upper");
    }

    /**
     * 列名保持原样
     *
     * @return
     */
    public DbModule normal() {
        return columnCase("default");
    }

    public String getType() {
        return VAR_NAME_MODULE_DB;
    }

    static class KeyHolder extends GeneratedKeyHolder {
        private final boolean useGeneratedKeys;
        private final String primary;

        public KeyHolder() {
            this(null);
        }

        public KeyHolder(String primary) {
            this.primary = primary;
            this.useGeneratedKeys = StringUtils.isBlank(primary);
        }

        PreparedStatement createPreparedStatement(Connection connection, String sql) throws SQLException {
            if (useGeneratedKeys) {
                return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }
            return connection.prepareStatement(sql, new String[]{primary});
        }

        public Object getObjectKey() {
            List<Map<String, Object>> keyList = getKeyList();
            if (keyList.isEmpty()) {
                return null;
            }
            Iterator<Object> keyIterator = keyList.get(0).values().iterator();
            Object key = keyIterator.hasNext() ? keyIterator.next() : null;
            return key;
        }
    }
}
