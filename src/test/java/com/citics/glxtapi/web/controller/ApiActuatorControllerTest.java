package com.citics.glxtapi.web.controller;

import com.citics.glxtapi.plugin.db.exception.OpenException;
import com.citics.glxtapi.web.service.ApiActuatorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class ApiActuatorControllerTest {

    private static final String REQUEST_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"params\":{}}";
    private static final String EXPORT_REQUEST_BODY = "{\"tenant\":\"demo\",\"apiCode\":\"demoApi\",\"token\":\"token\",\"exportExcel\":true,\"params\":{}}";

    @Mock
    private ApiActuatorService apiActuatorService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ApiActuatorController controller = new ApiActuatorController();
        controller.apiActuatorService = apiActuatorService;
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void executeReturnsDataAndWritesSuccessLog() throws Exception {
        when(apiActuatorService.execute(eq(REQUEST_BODY), any(HttpServletRequest.class))).thenReturn(Collections.singletonMap("id", 1));

        mockMvc.perform(post("/api/actuator/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", is(1)));

        verify(apiActuatorService).execute(eq(REQUEST_BODY), any(HttpServletRequest.class));
        verify(apiActuatorService).insertAfterExecute(eq(REQUEST_BODY), any(HttpServletRequest.class), eq(true), eq(null), anyLong());
    }

    @Test
    public void executeReturnsFileNameWhenExportExcelIsTrue() throws Exception {
        String fileName = "uuid_demoApi_20260630153000.xlsx";
        when(apiActuatorService.execute(eq(EXPORT_REQUEST_BODY), any(HttpServletRequest.class))).thenReturn(fileName);

        mockMvc.perform(post("/api/actuator/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXPORT_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", is(fileName)));

        verify(apiActuatorService).execute(eq(EXPORT_REQUEST_BODY), any(HttpServletRequest.class));
        verify(apiActuatorService).insertAfterExecute(eq(EXPORT_REQUEST_BODY), any(HttpServletRequest.class), eq(true), eq(null), anyLong());
    }

    @Test
    public void executeReturnsErrorJsonAndWritesFailureLog() throws Exception {
        doThrow(new OpenException("no permission")).when(apiActuatorService).execute(eq(REQUEST_BODY), any(HttpServletRequest.class));

        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            mockMvc.perform(post("/api/actuator/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REQUEST_BODY))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code", is(1002)))
                    .andExpect(jsonPath("$.message", containsString("no permission")));
        } finally {
            System.setErr(originalErr);
        }

        ArgumentCaptor<Object> detailCaptor = ArgumentCaptor.forClass(Object.class);
        verify(apiActuatorService).insertAfterExecute(eq(REQUEST_BODY), any(HttpServletRequest.class), eq(false), detailCaptor.capture(), anyLong());
        assertEquals("no permission", detailCaptor.getValue());
    }
}
