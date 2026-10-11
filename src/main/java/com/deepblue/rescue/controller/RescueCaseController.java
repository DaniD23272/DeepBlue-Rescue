package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.service.RescueCaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rescue-cases")
public class RescueCaseController {

    private final RescueCaseService rescueCaseService;

    public RescueCaseController(RescueCaseService rescueCaseService) {
        this.rescueCaseService = rescueCaseService;
    }

    @GetMapping("/{caseCode}")
    public ResponseEntity<RescueCaseResponse> findByCode(
            @PathVariable String caseCode) {
        return ResponseEntity.ok(
                rescueCaseService.findByCode(caseCode)
        );
    }

    @GetMapping
    public ResponseEntity<List<RescueCaseResponse>> findByStatus(
            @RequestParam RescueStatus status) {
        return ResponseEntity.ok(
                rescueCaseService.findByStatus(status)
        );
    }

    @PatchMapping("/{caseCode}/status")
    public ResponseEntity<RescueCaseResponse> changeStatus(
            @PathVariable String caseCode,
            @Valid @RequestBody ChangeRescueStatusRequest request) {
        return ResponseEntity.ok(
                rescueCaseService.changeStatus(caseCode, request)
        );
    }
}