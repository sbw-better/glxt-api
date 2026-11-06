package com.citics.glxtapi.web.service.impl;

import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import org.junit.Test;

import java.util.Arrays;

import static com.citics.glxtapi.web.constants.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiServiceImplProcedureJdbcTypeTest {

    @Test
    public void resolvesPersistedDictionaryValuesToJdbcTypeNames() {
        assertEquals(PROCEDURE_JDBC_TYPE_VARCHAR, getProcedureJdbcTypeName(1));
        assertEquals(PROCEDURE_JDBC_TYPE_INTEGER, getProcedureJdbcTypeName(2));
        assertEquals(PROCEDURE_JDBC_TYPE_BIGINT, getProcedureJdbcTypeName(3));
        assertEquals(PROCEDURE_JDBC_TYPE_DECIMAL, getProcedureJdbcTypeName(4));
        assertEquals(PROCEDURE_JDBC_TYPE_DATE, getProcedureJdbcTypeName(5));
        assertEquals(PROCEDURE_JDBC_TYPE_TIMESTAMP, getProcedureJdbcTypeName(6));
        assertEquals(PROCEDURE_JDBC_TYPE_CLOB, getProcedureJdbcTypeName(7));
        assertEquals(PROCEDURE_JDBC_TYPE_CURSOR, getProcedureJdbcTypeName(8));
        assertEquals(null, getProcedureJdbcTypeName(99));
    }

    @Test
    public void requiresProcedureOrderNumbersToStartAtOneAndBeContinuous() {
        assertTrue(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(1, 2, 3)), 3));
        assertFalse(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(1, 3)), 2));
        assertFalse(ApiServiceImpl.hasContinuousProcedureOrderNo(
                new java.util.HashSet<>(java.util.Arrays.asList(0, 1)), 2));
    }

    @Test
    public void previewDisplaysJdbcTypeStringsWhenFrontendSubmitsDictionaryValues() {
        ApiParam input = new ApiParam();
        input.setCode("content");
        input.setDirection(1);
        input.setJdbcType(7);
        input.setOrderNo(1);

        ApiParam output = new ApiParam();
        output.setCode("data");
        output.setDirection(2);
        output.setJdbcType(8);
        output.setOrderNo(2);

        ApiInterfaceDTO dto = new ApiInterfaceDTO();
        dto.setType(2);
        dto.setProcedureName("PKG_EMP.QUERY_EMP");
        dto.setApiParamList(Arrays.asList(input, output));

        assertEquals("{ call PKG_EMP.QUERY_EMP(content, data) }",
                new ApiServiceImpl().preview(dto));
    }
}
