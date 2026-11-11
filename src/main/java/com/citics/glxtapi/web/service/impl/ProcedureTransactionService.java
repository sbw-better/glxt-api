package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.plugin.sql.DbModule;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.vo.ProcedureExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 独立的存储过程事务边界。
 * 由 ApiActuatorServiceImpl 在选定动态数据源后通过 Spring 代理调用，确保事务
 * 管理器在开启事务时从动态数据源取得的连接与 JdbcTemplate 执行过程时使用同一连接。
 */
@Service
@Slf4j
public class ProcedureTransactionService {

    @Resource
    private DbModule dbModule;

    /**
     * 存储过程执行期间出现异常时回滚数据库事务。异常继续向上传播，便于事务拦截器
     * 在方法退出前完成回滚；接口层再负责转换为对外错误响应。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcedureExecuteResult execute(String procedureName, List<ApiParam> params,
                                          Map<String, Object> inputValues, Integer fieldBackMode) {
        log.info("开始事务执行存储过程，procedureName={}, paramCount={}", procedureName,
                params == null ? 0 : params.size());
        try {
            ProcedureExecuteResult result = dbModule.callProcedure(procedureName, params,
                    inputValues, true, fieldBackMode);
            log.info("存储过程执行成功，procedureName={}, resultCount={}", procedureName,
                    result == null ? null : result.getResultCount());
            return result;
        } catch (RuntimeException e) {
            // 不吞异常：异常需离开此代理方法，事务拦截器才能执行回滚。
            log.error("存储过程执行异常，事务将回滚，procedureName={}, exceptionType={}",
                    procedureName, e.getClass().getName(), e);
            throw e;
        }
    }
}
