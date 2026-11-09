package com.citics.glxtapi.web.service.impl;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.citics.glxtapi.web.entity.ApiParam;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import com.citics.glxtapi.web.service.ConnectionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_IN;
import static com.citics.glxtapi.web.constants.Constants.PROCEDURE_PARAM_DIRECTION_OUT;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImplProcedureValidationTest {

    @Mock
    private ConnectionService connectionService;

    private ApiServiceImpl service;

    @Before
    public void setUp() {
        service = new ApiServiceImpl();
        ReflectionTestUtils.setField(service, "connectionService", connectionService);
        when(connectionService.isHaveConnectionPermission("demo", 1L)).thenReturn(true);
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsOrderNumbersWithGapsBeforeTheyCanBindTheWrongPosition() {
        validate(Arrays.asList(
                procedureParam("first", PROCEDURE_PARAM_DIRECTION_IN, 1, 1),
                procedureParam("third", PROCEDURE_PARAM_DIRECTION_IN, 1, 3)
        ));
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsDuplicateProcedureParameterCodes() {
        validate(Arrays.asList(
                procedureParam("same", PROCEDURE_PARAM_DIRECTION_IN, 1, 1),
                procedureParam("same", PROCEDURE_PARAM_DIRECTION_IN, 1, 2)
        ));
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsJdbcTypeOutsideTheDictionaryRange() {
        validate(Arrays.asList(
                procedureParam("id", PROCEDURE_PARAM_DIRECTION_IN, 9, 1)
        ));
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsCursorForInoutDirection() {
        validate(Arrays.asList(
                procedureParam("data", 3, 6, 1)
        ));
    }

    @Test
    public void acceptsCursorWithoutGeneralParameterTypeAndKeepsNumericJdbcTypeForPersistence() {
        ApiParam input = procedureParam("id", PROCEDURE_PARAM_DIRECTION_IN, 1, 1);
        ApiParam cursor = procedureParam("data", PROCEDURE_PARAM_DIRECTION_OUT, 6, 2);

        validate(Arrays.asList(input, cursor));

        org.junit.Assert.assertEquals(Integer.valueOf(1), input.getJdbcType());
        org.junit.Assert.assertEquals(Integer.valueOf(6), cursor.getJdbcType());
    }

    private void validate(List<ApiParam> params) {
        ApiInterfaceDTO dto = new ApiInterfaceDTO();
        dto.setProcedureName("PKG.PROC");
        dto.setConnectionId(1L);
        ReflectionTestUtils.invokeMethod(service, "validateProcedureApi", dto, params, "demo");
    }

    private ApiParam procedureParam(String code, Integer direction, Integer jdbcType, Integer orderNo) {
        ApiParam param = new ApiParam();
        param.setName(code);
        param.setCode(code);
        param.setDirection(direction);
        param.setJdbcType(jdbcType);
        param.setOrderNo(orderNo);
        return param;
    }
}
