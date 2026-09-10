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

import static com.citics.glxtapi.web.constants.Constants.FILED_TYPE_INT;
import static com.citics.glxtapi.web.constants.Constants.FILED_TYPE_LIST;
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
                procedureParam("first", PROCEDURE_PARAM_DIRECTION_IN, "DECIMAL", FILED_TYPE_INT, 1),
                procedureParam("third", PROCEDURE_PARAM_DIRECTION_IN, "DECIMAL", FILED_TYPE_INT, 3)
        ));
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsCursorWhenBusinessTypeIsNotList() {
        validate(Arrays.asList(
                procedureParam("data", PROCEDURE_PARAM_DIRECTION_OUT, "CURSOR", FILED_TYPE_INT, 1)
        ));
    }

    @Test(expected = MybatisPlusException.class)
    public void rejectsDuplicateProcedureParameterCodes() {
        validate(Arrays.asList(
                procedureParam("same", PROCEDURE_PARAM_DIRECTION_IN, "DECIMAL", FILED_TYPE_INT, 1),
                procedureParam("same", PROCEDURE_PARAM_DIRECTION_IN, "DECIMAL", FILED_TYPE_INT, 2)
        ));
    }

    @Test
    public void acceptsCursorListAndNormalizesNumericJdbcTypeBeforePersistence() {
        ApiParam input = procedureParam("id", PROCEDURE_PARAM_DIRECTION_IN, "4", FILED_TYPE_INT, 1);
        ApiParam cursor = procedureParam("data", PROCEDURE_PARAM_DIRECTION_OUT, "8", FILED_TYPE_LIST, 2);

        validate(Arrays.asList(input, cursor));

        org.junit.Assert.assertEquals("DECIMAL", input.getJdbcType());
        org.junit.Assert.assertEquals("CURSOR", cursor.getJdbcType());
    }

    private void validate(List<ApiParam> params) {
        ApiInterfaceDTO dto = new ApiInterfaceDTO();
        dto.setProcedureName("PKG.PROC");
        dto.setConnectionId(1L);
        ReflectionTestUtils.invokeMethod(service, "validateProcedureApi", dto, params, "demo");
    }

    private ApiParam procedureParam(String code, Integer direction, String jdbcType, Integer type, Integer orderNo) {
        ApiParam param = new ApiParam();
        param.setName(code);
        param.setCode(code);
        param.setDirection(direction);
        param.setJdbcType(jdbcType);
        param.setType(type);
        param.setOrderNo(orderNo);
        return param;
    }
}
