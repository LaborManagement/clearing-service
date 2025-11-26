package com.example.clearing.service;

import com.example.clearing.domain.BankTransaction;
import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.model.AllocationRequest;
import com.example.clearing.model.AllocationResponse;
import com.example.clearing.repository.BankTransactionRepository;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.shared.common.dao.TenantAccessDao;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class AllocationService {

    private static final String STATUS_TYPE_ALLOCATION = "payment_allocation";
    private static final String STATUS_CODE_ALLOCATED = "ALLOCATED";
    private static final String STATUS_TYPE_BANK_TXN = "bank_transaction";
    private static final String STATUS_CODE_ALLOCATED_TXN = "ALLOCATED";

    private final BankTransactionRepository bankTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;

    public AllocationService(
            BankTransactionRepository bankTransactionRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            StatusService statusService,
            TenantAccessDao tenantAccessDao) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.statusService = statusService;
        this.tenantAccessDao = tenantAccessDao;
    }

    @Transactional
    public AllocationResponse createAllocation(AllocationRequest request) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
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
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + request.getBankTxnId()));

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
        txn.setStatusId(statusService.requireStatusId(STATUS_TYPE_BANK_TXN, STATUS_CODE_ALLOCATED_TXN));

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
        return toResponse(saved, txn);
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
