package com.example.clearing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.example.clearing.domain.BankTransaction;
import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.RequestSettlement;
import com.example.clearing.dto.AllocationBreakdown;
import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.model.AllocationRequest;
import com.example.clearing.model.AllocationResponse;
import com.example.clearing.repository.BankTransactionRepository;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.RequestSettlementRepository;
import com.shared.common.dao.TenantAccessDao;

import jakarta.transaction.Transactional;

@Service
public class AllocationService {

    private static final String STATUS_TYPE_ALLOCATION = "payment_allocation";
    private static final String STATUS_CODE_ALLOCATED = "ALLOCATED";
    private static final String STATUS_TYPE_BANK_TXN = "bank_transaction";
    private static final int STATUS_ID_ALLOCATED_TXN = 2;
    private static final int STATUS_ID_SETTLED_TXN = 3;

    private final BankTransactionRepository bankTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final RequestSettlementRepository requestSettlementRepository;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;
    private final SettlementService settlementService;

    public AllocationService(
            BankTransactionRepository bankTransactionRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            RequestSettlementRepository requestSettlementRepository,
            StatusService statusService,
            TenantAccessDao tenantAccessDao,
            SettlementService settlementService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.requestSettlementRepository = requestSettlementRepository;
        this.statusService = statusService;
        this.tenantAccessDao = tenantAccessDao;
        this.settlementService = settlementService;
    }

    @Transactional
    public AllocationResponse createAllocation(AllocationRequest request) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        if (request.getRequestedAmount() == null || request.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("requestedAmount must be > 0");
        }
        // Idempotent check
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<PaymentAllocation> existing = paymentAllocationRepository.findByIdempotencyKey(
                    request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                return toResponse(existing.get(), null);
            }
        }

        Optional<PaymentAllocation> existingPair = paymentAllocationRepository.findByRequestIdAndBankTxnId(
                request.getRequestId(), request.getBankTxnId());
        if (existingPair.isPresent()) {
            throw new IllegalStateException("Allocation already exists for this request and bank transaction");
        }

        BankTransaction txn = bankTransactionRepository
                .findById(request.getBankTxnId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Bank transaction not found: " + request.getBankTxnId()));

        BigDecimal amount = request.getAllocatedAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("allocatedAmount must be > 0");
        }

        BigDecimal remaining = txn.getRemainingAmount() == null ? BigDecimal.ZERO : txn.getRemainingAmount();
        if (remaining.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient remaining amount on bank transaction");
        }

        OffsetDateTime now = OffsetDateTime.now();
        txn.setAllocatedAmount(
                (txn.getAllocatedAmount() == null ? BigDecimal.ZERO : txn.getAllocatedAmount()).add(amount));
        txn.setRemainingAmount(remaining.subtract(amount));
        txn.setUpdatedAt(now);
        boolean nowSettled = txn.getRemainingAmount() != null
                && txn.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0;
        txn.setIsSettled(nowSettled);
        txn.setStatusId(nowSettled ? STATUS_ID_SETTLED_TXN : STATUS_ID_ALLOCATED_TXN);
        txn.setStatusCode(statusService.resolveStatusCode(STATUS_TYPE_BANK_TXN, txn.getStatusId()));

        try {
            bankTransactionRepository.saveAndFlush(txn);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Concurrent update detected on bank transaction", ex);
        }

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setRequestId(request.getRequestId());
        allocation.setBankTxnId(request.getBankTxnId());
        allocation.setAllocatedAmount(amount);
        allocation.setAllocationDate(
                request.getAllocationDate() != null ? request.getAllocationDate() : LocalDate.now());
        allocation.setAllocatedBy(request.getAllocatedBy());
        allocation.setStatus(STATUS_CODE_ALLOCATED);
        allocation.setStatusId(statusService.requireStatusId(STATUS_TYPE_ALLOCATION, STATUS_CODE_ALLOCATED));
        allocation.setCreatedAt(now);
        allocation.setUpdatedAt(now);
        allocation.setIdempotencyKey(request.getIdempotencyKey());
        allocation.setBoardId(tenantAccess.boardId);
        allocation.setEmployerId(tenantAccess.employerId);
        allocation.setToliId(tenantAccess.toliId);

        PaymentAllocation saved = paymentAllocationRepository.save(allocation);
        RequestSettlement rs = updateRequestSettlement(request, amount, tenantAccess, now);
        autoFinalizeIfSettled(rs, tenantAccess);
        return toResponse(saved, txn);
    }

    private RequestSettlement updateRequestSettlement(
            AllocationRequest request, BigDecimal allocAmount, TenantAccessDao.TenantAccess tenantAccess,
            OffsetDateTime now) {
        RequestSettlement rs = requestSettlementRepository.findByRequestId(request.getRequestId())
                .orElseGet(() -> {
                    RequestSettlement created = new RequestSettlement();
                    created.setRequestId(request.getRequestId());
                    created.setBoardId(tenantAccess.boardId.longValue());
                    created.setEmployerId(tenantAccess.employerId.longValue());
                    created.setToliId(tenantAccess.toliId != null ? tenantAccess.toliId.longValue() : null);
                    created.setTotalAmount(request.getRequestedAmount());
                    created.setAllocatedAmount(BigDecimal.ZERO);
                    created.setRemainingAmount(request.getRequestedAmount() != null
                            ? request.getRequestedAmount()
                            : BigDecimal.ZERO);
                    created.setCreatedAt(now);
                    created.setUpdatedAt(now);
                    return created;
                });

        if (!tenantAccess.boardId.equals(rs.getBoardId().intValue())
                || !tenantAccess.employerId.equals(rs.getEmployerId().intValue())) {
            throw new IllegalStateException("Request settlement tenant mismatch for request " + request.getRequestId());
        }
        if (request.getRequestedAmount() != null
                && rs.getTotalAmount() != null
                && rs.getTotalAmount().compareTo(request.getRequestedAmount()) != 0) {
            throw new IllegalArgumentException("Requested amount does not match existing total for request");
        }

        BigDecimal newAllocated = rs.getAllocatedAmount().add(allocAmount);
        if (rs.getTotalAmount() != null && newAllocated.compareTo(rs.getTotalAmount()) > 0) {
            throw new IllegalStateException("Allocations exceed request total for request " + request.getRequestId());
        }

        rs.setAllocatedAmount(newAllocated);
        rs.setRemainingAmount(
                rs.getTotalAmount() != null ? rs.getTotalAmount().subtract(newAllocated) : BigDecimal.ZERO);
        rs.setUpdatedAt(now);
        rs.setStatusId(statusService.requireStatusId("request_settlement",
                rs.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "ALLOCATED"));

        requestSettlementRepository.save(rs);
        return rs;
    }

    private void autoFinalizeIfSettled(RequestSettlement rs, TenantAccessDao.TenantAccess tenantAccess) {
        if (rs.getRemainingAmount().compareTo(BigDecimal.ZERO) != 0) {
            return;
        }
        // Build allocations payload from unlinked payment allocations
        java.util.List<PaymentAllocation> pending = paymentAllocationRepository.findByRequestIdAndVoucherIdIsNull(
                rs.getRequestId());
        if (pending.isEmpty()) {
            return;
        }
        SettlementRequest settlementRequest = new SettlementRequest();
        settlementRequest.setRequestId(rs.getRequestId());
        settlementRequest.setBoardId(rs.getBoardId());
        settlementRequest.setEmployerId(rs.getEmployerId());
        settlementRequest.setTotalAmount(rs.getTotalAmount());
        settlementRequest.setIdempotencyKey("REQ-" + rs.getRequestId() + "-AUTO");

        List<AllocationBreakdown> allocations = pending.stream()
                .map(pa -> {
                    AllocationBreakdown ab = new AllocationBreakdown();
                    ab.setBankTxnId(pa.getBankTxnId().longValue());
                    ab.setAmount(pa.getAllocatedAmount());
                    return ab;
                })
                .toList();
        settlementRequest.setAllocations(allocations);

        settlementService.processSettlement(settlementRequest);
    }

    private AllocationResponse toResponse(PaymentAllocation allocation, BankTransaction txn) {
        AllocationResponse response = new AllocationResponse();
        response.setAllocationId(allocation.getAllocationId());
        response.setRequestId(allocation.getRequestId());
        response.setBankTxnId(allocation.getBankTxnId());
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setAllocationDate(allocation.getAllocationDate());
        response.setStatus(allocation.getStatus());
        if (txn != null) {
            response.setRemainingAmount(txn.getRemainingAmount());
        }
        return response;
    }

    private TenantAccessDao.TenantAccess requireTenantAccess() {
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) assigned for allocation");
        }
        return ta;
    }
}
