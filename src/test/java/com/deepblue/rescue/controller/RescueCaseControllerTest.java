package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService rescueCaseService;

    private RescueCaseResponse sampleCase(RescueStatus status) {
        return new RescueCaseResponse(
                1L, "RC-001", LocalDate.of(2026, 1, 15), "North beach",
                status, "CENTER-01", "AN-001"
        );
    }

    @Test
    void findByCodeReturns200AndDelegatesToService() throws Exception {
        RescueStatus rescueStatus = RescueStatus.values()[0];
        when(rescueCaseService.findByCode("RC-001")).thenReturn(sampleCase(rescueStatus));

        mockMvc.perform(get("/api/rescue-cases/RC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RC-001"))
                .andExpect(jsonPath("$.centerCode").value("CENTER-01"));

        verify(rescueCaseService).findByCode("RC-001");
    }

    @Test
    void findByStatusReturns200AndDelegatesToService() throws Exception {
        RescueStatus rescueStatus = RescueStatus.values()[0];
        when(rescueCaseService.findByStatus(rescueStatus))
                .thenReturn(List.of(sampleCase(rescueStatus)));

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", rescueStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseCode").value("RC-001"));

        verify(rescueCaseService).findByStatus(rescueStatus);
    }

    @Test
    void changeStatusReturns200AndDelegatesToService() throws Exception {
        RescueStatus newStatus = RescueStatus.values()[0];
        when(rescueCaseService.changeStatus(eq("RC-001"), any(ChangeRescueStatusRequest.class)))
                .thenReturn(sampleCase(newStatus));

        mockMvc.perform(patch("/api/rescue-cases/RC-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RC-001"));

        verify(rescueCaseService).changeStatus(
                eq("RC-001"),
                org.mockito.ArgumentMatchers.argThat(request -> request.status() == newStatus)
        );
    }

    @Test
    void changeStatusWithNullStatusReturns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/RC-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());

        verify(rescueCaseService, never()).changeStatus(eq("RC-001"), any());
    }

    @Test
    void missingCaseReturns404() throws Exception {
        when(rescueCaseService.findByCode("MISSING"))
                .thenThrow(new ResourceNotFoundException("Rescue case not found"));

        mockMvc.perform(get("/api/rescue-cases/MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidStatusTransitionReturns409() throws Exception {
        RescueStatus newStatus = RescueStatus.values()[0];
        when(rescueCaseService.changeStatus(eq("RC-001"), any(ChangeRescueStatusRequest.class)))
                .thenThrow(new BusinessRuleException("Status transition is not allowed"));

        mockMvc.perform(patch("/api/rescue-cases/RC-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus.name() + "\"}"))
                .andExpect(status().isConflict());
    }
}
