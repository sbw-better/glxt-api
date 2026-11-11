package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.plugin.sql.DbModule;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcedureTransactionServiceTest {

    @Test
    public void executeUsesRollbackForExceptionAndPropagatesDatabaseFailure() throws Exception {
        Transactional transactional = ProcedureTransactionService.class
                .getMethod("execute", String.class, java.util.List.class, java.util.Map.class, Integer.class)
                .getAnnotation(Transactional.class);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);

        DbModule dbModule = mock(DbModule.class);
        ProcedureTransactionService service = new ProcedureTransactionService();
        ReflectionTestUtils.setField(service, "dbModule", dbModule);
        RuntimeException failure = new RuntimeException("procedure failed");
        when(dbModule.callProcedure("PKG.PROC", null, null, true, null)).thenThrow(failure);

        try {
            service.execute("PKG.PROC", null, null, null);
            fail("Expected procedure failure to propagate to the transaction interceptor");
        } catch (RuntimeException actual) {
            assertSame(failure, actual);
        }
        verify(dbModule).callProcedure("PKG.PROC", null, null, true, null);
    }
}
