package com.example.clearing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.dto.SettlementResponse;
import com.example.clearing.service.SettlementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clearing/events")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/finalize")
    public ResponseEntity<SettlementResponse> finalizeSettlement(@Valid @RequestBody SettlementRequest request) {
        SettlementResponse response = settlementService.processSettlement(request);
        return ResponseEntity.ok(response);
    }
}
