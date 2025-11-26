package com.example.clearing.controller;

import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.RequestSettlement;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.RequestSettlementRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clearing")
@SecurityRequirement(name = "Bearer Authentication")
public class QueryController {

    private static final int MAX_LIMIT = 200;

    private final VoucherHeaderRepository voucherHeaderRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final RequestSettlementRepository requestSettlementRepository;

    public QueryController(
            VoucherHeaderRepository voucherHeaderRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            RequestSettlementRepository requestSettlementRepository) {
        this.voucherHeaderRepository = voucherHeaderRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.requestSettlementRepository = requestSettlementRepository;
    }

    @GetMapping("/voucher-headers")
    public ResponseEntity<List<VoucherHeader>> getVoucherHeaders(
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) Integer employerId,
            @RequestParam(required = false) String voucherNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<VoucherHeader> result = voucherHeaderRepository.search(
                boardId, employerId, voucherNumber, status, PageRequest.of(0, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/payment-allocations")
    public ResponseEntity<List<PaymentAllocation>> getPaymentAllocations(
            @RequestParam(required = false) Long requestId,
            @RequestParam(required = false) Integer bankTxnId,
            @RequestParam(required = false) Integer voucherId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<PaymentAllocation> result = paymentAllocationRepository.search(
                requestId, bankTxnId, voucherId, status, PageRequest.of(0, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/request-settlements")
    public ResponseEntity<List<RequestSettlement>> getRequestSettlements(
            @RequestParam(required = false) Long requestId,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<RequestSettlement> result = requestSettlementRepository.search(
                requestId, boardId, employerId, status, PageRequest.of(0, size));
        return ResponseEntity.ok(result);
    }
}
