package com.example.clearing.controller;

import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.RequestSettlement;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.RequestSettlementRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import com.example.clearing.service.StatusService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clearing-service/api/clearing")
@SecurityRequirement(name = "Bearer Authentication")
public class QueryController {

    private static final int MAX_LIMIT = 200;

    private final VoucherHeaderRepository voucherHeaderRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final RequestSettlementRepository requestSettlementRepository;
    private final StatusService statusService;

    public QueryController(
            VoucherHeaderRepository voucherHeaderRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            RequestSettlementRepository requestSettlementRepository,
            StatusService statusService) {
        this.voucherHeaderRepository = voucherHeaderRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.requestSettlementRepository = requestSettlementRepository;
        this.statusService = statusService;
    }

    @GetMapping("/voucher-headers")
    public ResponseEntity<?> getVoucherHeaders(
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false) Integer employerId,
            @RequestParam(required = false) String voucherNumber,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(name = "status", required = false) String statusCode,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            int size = Math.max(1, Math.min(limit, MAX_LIMIT));
            Integer resolvedStatusId = resolveStatusId(statusCode, statusId, "voucher_header");
            List<VoucherHeader> result = voucherHeaderRepository.search(
                    boardId, employerId, voucherNumber, resolvedStatusId, PageRequest.of(0, size));
            result.forEach(vh -> vh.setStatus(statusService.resolveStatusCode("voucher_header", vh.getStatusId())));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/payment-allocations")
    public ResponseEntity<?> getPaymentAllocations(
            @RequestParam(required = false) Long requestId,
            @RequestParam(required = false) Integer bankTxnId,
            @RequestParam(required = false) Integer voucherId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(name = "status", required = false) String statusCode,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            int size = Math.max(1, Math.min(limit, MAX_LIMIT));
            Integer resolvedStatusId = resolveStatusId(statusCode, statusId, "payment_allocation");
            List<PaymentAllocation> result = paymentAllocationRepository.search(
                    requestId, bankTxnId, voucherId, resolvedStatusId, PageRequest.of(0, size));
            result.forEach(pa -> pa.setStatus(statusService.resolveStatusCode("payment_allocation", pa.getStatusId())));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/request-settlements")
    public ResponseEntity<?> getRequestSettlements(
            @RequestParam(required = false) Long requestId,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(name = "status", required = false) String statusCode,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            int size = Math.max(1, Math.min(limit, MAX_LIMIT));
            Integer resolvedStatusId = resolveStatusId(statusCode, statusId, "request_settlement");
            List<RequestSettlement> result = requestSettlementRepository.search(
                    requestId, boardId, employerId, resolvedStatusId, PageRequest.of(0, size));
            result.forEach(rs -> rs.setStatus(statusService.resolveStatusCode("request_settlement", rs.getStatusId())));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private Integer resolveStatusId(String statusCode, Integer statusId, String statusType) {
        if (statusId != null) {
            return statusId;
        }
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        return statusService.requireStatusId(statusType, statusCode.trim());
    }
}
