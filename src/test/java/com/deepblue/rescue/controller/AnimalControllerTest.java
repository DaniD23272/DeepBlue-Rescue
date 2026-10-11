package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentEligibilityResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse sampleAnimal() {
        return new AnimalResponse(
                1L, "AN-001", "Green turtle", "Chelonia mydas",
                AnimalSex.values()[0], RescueStatus.values()[0]
        );
    }

    private TreatmentResponse sampleTreatment() {
        return new TreatmentResponse(
                10L, "AN-001", "SP-001", LocalDateTime.of(2026, 1, 10, 9, 0),
                com.deepblue.rescue.domain.TreatmentType.values()[0],
                "Routine medical examination"
        );
    }

    @Test
    void findByCodeReturns200AndDelegatesToService() throws Exception {
        when(animalService.findByCode("AN-001")).thenReturn(sampleAnimal());

        mockMvc.perform(get("/api/animals/AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.commonName").value("Green turtle"));

        verify(animalService).findByCode("AN-001");
    }

    @Test
    void findAnimalsInRehabilitationReturns200AndList() throws Exception {
        when(animalService.findAnimalsInRehabilitation()).thenReturn(List.of(sampleAnimal()));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"));

        verify(animalService).findAnimalsInRehabilitation();
        verify(animalService, never()).findByCode("in-rehabilitation");
    }

    @Test
    void findTreatmentsByAnimalCodeReturns200AndList() throws Exception {
        when(treatmentService.findByAnimalCode("AN-001")).thenReturn(List.of(sampleTreatment()));

        mockMvc.perform(get("/api/animals/AN-001/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"))
                .andExpect(jsonPath("$[0].description").value("Routine medical examination"));

        verify(treatmentService).findByAnimalCode("AN-001");
    }

    @Test
    void eligibilityReturns200WithEligibilityResult() throws Exception {
        when(animalService.canReceiveTreatment("AN-001")).thenReturn(true);

        mockMvc.perform(get("/api/animals/AN-001/treatment-eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    @Test
    void missingAnimalReturns404() throws Exception {
        when(animalService.findByCode("MISSING"))
                .thenThrow(new ResourceNotFoundException("Animal not found"));

        mockMvc.perform(get("/api/animals/MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAnimalEligibilityReturns404() throws Exception {
        when(animalService.canReceiveTreatment("MISSING"))
                .thenThrow(new ResourceNotFoundException("Animal not found"));

        mockMvc.perform(get("/api/animals/MISSING/treatment-eligibility"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAnimalTreatmentsReturns404() throws Exception {
        when(treatmentService.findByAnimalCode("MISSING"))
                .thenThrow(new ResourceNotFoundException("Animal not found"));

        mockMvc.perform(get("/api/animals/MISSING/treatments"))
                .andExpect(status().isNotFound());
    }
}
