package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService treatmentService;

    private TreatmentResponse sampleTreatment() {
        return new TreatmentResponse(
                10L, "AN-001", "SP-001", LocalDateTime.of(2026, 1, 10, 9, 0),
                TreatmentType.values()[0], "Routine medical examination"
        );
    }

    @Test
    void registerValidTreatmentReturns201AndDelegatesToService() throws Exception {
        TreatmentType type = TreatmentType.values()[0];
        when(treatmentService.register(any(CreateTreatmentRequest.class)))
                .thenReturn(sampleTreatment());
        String request = """
                {
                  "animalCode": "AN-001",
                  "specialistCode": "SP-001",
                  "performedAt": "2026-01-10T09:00:00",
                  "type": "%s",
                  "description": "Routine medical examination"
                }
                """.formatted(type.name());

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.specialistCode").value("SP-001"));

        verify(treatmentService).register(org.mockito.ArgumentMatchers.argThat(body ->
                body.animalCode().equals("AN-001")
                        && body.specialistCode().equals("SP-001")
                        && body.type() == type
                        && body.description().equals("Routine medical examination")
        ));
    }

    @Test
    void invalidTreatmentRequestReturns400AndDoesNotCallService() throws Exception {
        String invalidRequest = """
                {
                  "animalCode": "",
                  "specialistCode": "",
                  "performedAt": "2099-01-01T00:00:00",
                  "type": null,
                  "description": "x"
                }
                """;

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(treatmentService, never()).register(any());
    }

    @Test
    void registerRequestWithoutDescriptionReturns400() throws Exception {
        TreatmentType type = TreatmentType.values()[0];
        String invalidRequest = """
                {
                  "animalCode": "AN-001",
                  "specialistCode": "SP-001",
                  "performedAt": "2026-01-10T09:00:00",
                  "type": "%s",
                  "description": "   "
                }
                """.formatted(type.name());

        mockMvc.perform(post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(treatmentService, never()).register(any());
    }
}
