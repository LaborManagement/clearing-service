package com.example.clearing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.clearing.client.PaymentFlowClient;
import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.RequestSettlement;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.dto.AllocationBreakdown;
import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.dto.SettlementResponse;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.RequestSettlementRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import com.shared.common.dao.TenantAccessDao;
import com.shared.utilities.logger.LoggerFactoryProvider;

import jakarta.transaction.Transactional;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactoryProvider.getLogger(SettlementService.class);

    private final VoucherHeaderRepository voucherHeaderRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final RequestSettlementRepository requestSettlementRepository;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;
    private final PaymentFlowClient paymentFlowClient;

    public SettlementService(
            VoucherHeaderRepository voucherHeaderRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            RequestSettlementRepository requestSettlementRepository,
            StatusService statusService,
            TenantAccessDao tenantAccessDao,
            PaymentFlowClient paymentFlowClient) {
        this.voucherHeaderRepository = voucherHeaderRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.requestSettlementRepository = requestSettlementRepository;
        this.statusService = statusService;
        this.tenantAccessDao = tenantAccessDao;
        this.paymentFlowClient = paymentFlowClient;
    }

    @Transactional
    public SettlementResponse processSettlement(SettlementRequest request) {
        TenantAccess tenantAccess = resolveTenantAccess(request);
        BigDecimal totalAmount = sumAllocations(request);
        if (request.getTotalAmount() != null && totalAmount.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Sum of allocations does not match totalAmount");
        }
        String voucherNumber = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : "REQ-" + request.getRequestId();

        VoucherHeader voucherHeader = voucherHeaderRepository
                .findFirstByBoardIdAndEmployerIdAndVoucherNumber(
                        tenantAccess.boardId, tenantAccess.employerId, voucherNumber)
                .orElseGet(() -> createVoucherHeader(voucherNumber, totalAmount, tenantAccess));

        OffsetDateTime now = OffsetDateTime.now();
        linkVoucherToPaymentAllocations(voucherHeader, request, tenantAccess, now);
        linkVoucherToRequestSettlement(voucherHeader, request.getRequestId(), tenantAccess, now);

        voucherHeader.setTotalAmount(totalAmount);
        voucherHeader.setUpdatedAt(now);
        voucherHeader.setStatusId(statusService.requireStatusId("voucher_header", "POSTED"));
        voucherHeaderRepository.save(voucherHeader);

        // Update payment status in payment-flow-service via internal API
        if (request.getRequestId() != null) {
            try {
                // Determine if this is a partial or full settlement
                RequestSettlement requestSettlement = requestSettlementRepository
                        .findByRequestId(request.getRequestId())
                        .orElse(null);

                Long statusId;
                if (requestSettlement != null
                        && requestSettlement.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
                    statusId = 5L; // RECONCILED - Fully settled/allocated
                    log.info("Full settlement detected for requestId: {}, updating to RECONCILED (statusId: 5)",
                            request.getRequestId());
                } else {
                    statusId = 4L; // PARTIALLY_RECONCILED - Partial settlement
                    log.info(
                            "Partial settlement detected for requestId: {}, updating to PARTIALLY_RECONCILED (statusId: 4)",
                            request.getRequestId());
                }

                paymentFlowClient.updatePaymentStatusById(request.getRequestId(), statusId);
                log.info("Successfully updated payment status for requestId: {} to statusId: {}",
                        request.getRequestId(), statusId);
            } catch (Exception e) {
                log.error(
                        "Failed to update payment status for requestId: {} - settlement completed but status sync failed",
                        request.getRequestId(), e);
                // Note: Settlement is still successful. This is a status sync issue.
                // Consider implementing a retry mechanism or reconciliation process.
            }
        }

        SettlementResponse response = new SettlementResponse();
        response.setEventId(null);
        response.setVoucherId(voucherHeader.getVoucherId());
        response.setVoucherStatus(statusService.resolveStatusCode("voucher_header", voucherHeader.getStatusId()));
        response.setTotalDebit(voucherHeader.getTotalAmount());
        response.setTotalCredit(voucherHeader.getTotalAmount());
        response.setMessage("Voucher posted");
        return response;
    }

    private VoucherHeader createVoucherHeader(String voucherNumber, BigDecimal totalAmount, TenantAccess tenantAccess) {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherHeader vh = new VoucherHeader();
        vh.setBoardId(tenantAccess.boardId);
        vh.setEmployerId(tenantAccess.employerId);
        vh.setToliId(tenantAccess.toliId);
        vh.setVoucherNumber(voucherNumber);
        vh.setVoucherDate(LocalDate.now());
        vh.setTotalAmount(totalAmount);
        vh.setStatusId(statusService.requireStatusId("voucher_header", "CREATED"));
        vh.setCreatedAt(now);
        vh.setUpdatedAt(now);
        return voucherHeaderRepository.save(vh);
    }

    private void linkVoucherToPaymentAllocations(
            VoucherHeader voucherHeader, SettlementRequest request, TenantAccess tenantAccess, OffsetDateTime now) {
        Map<Long, BigDecimal> amountsByTxn = request.getAllocations().stream()
                .collect(Collectors.groupingBy(
                        AllocationBreakdown::getBankTxnId,
                        Collectors.reducing(BigDecimal.ZERO, AllocationBreakdown::getAmount, BigDecimal::add)));

        var allocations = paymentAllocationRepository.findByRequestId(request.getRequestId());
        if (allocations.isEmpty()) {
            throw new IllegalStateException("No payment_allocation rows found for request " + request.getRequestId());
        }

        for (Map.Entry<Long, BigDecimal> entry : amountsByTxn.entrySet()) {
            Long bankTxnId = entry.getKey();
            BigDecimal required = entry.getValue();
            var candidates = allocations.stream()
                    .filter(a -> bankTxnId.equals(a.getBankTxnId().longValue())
                            && a.getVoucherId() == null
                            && tenantAccess.boardId.equals(a.getBoardId())
                            && tenantAccess.employerId.equals(a.getEmployerId()))
                    .sorted((a, b) -> a.getAllocationId().compareTo(b.getAllocationId()))
                    .toList();

            BigDecimal available = candidates.stream()
                    .map(PaymentAllocation::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(required) < 0) {
                throw new IllegalStateException("Not enough unlinked payment_allocation for txn " + bankTxnId);
            }

            BigDecimal remaining = required;
            for (PaymentAllocation pa : candidates) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal allocAmt = pa.getAllocatedAmount();
                if (remaining.compareTo(allocAmt) < 0) {
                    throw new IllegalStateException("Cannot partially consume allocation_id " + pa.getAllocationId());
                }
                pa.setVoucherId(voucherHeader.getVoucherId());
                pa.setUpdatedAt(now);
                pa.setStatusId(statusService.requireStatusId("payment_allocation", "SETTLED"));
                remaining = remaining.subtract(allocAmt);
            }

            if (remaining.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalStateException("Failed to fully link allocations for txn " + bankTxnId);
            }
        }

        paymentAllocationRepository.saveAll(allocations);
    }

    private TenantAccess resolveTenantAccess(SettlementRequest request) {
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) assigned for settlement");
        }
        if (request.getBoardId() != null && !request.getBoardId().equals(ta.boardId.longValue())) {
            throw new IllegalStateException("Requested board_id does not match user's tenant access");
        }
        if (request.getEmployerId() != null && !request.getEmployerId().equals(ta.employerId.longValue())) {
            throw new IllegalStateException("Requested employer_id does not match user's tenant access");
        }
        return new TenantAccess(ta.boardId, ta.employerId, ta.toliId);
    }

    private record TenantAccess(Integer boardId, Integer employerId, Integer toliId) {
    }

    private BigDecimal sumAllocations(SettlementRequest request) {
        return request.getAllocations().stream()
                .map(AllocationBreakdown::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void linkVoucherToRequestSettlement(
            VoucherHeader voucherHeader, Long requestId, TenantAccess tenantAccess, OffsetDateTime now) {
        if (requestId == null) {
            return;
        }
        requestSettlementRepository.findByRequestId(requestId)
                .ifPresent(rs -> {
                    if (!tenantAccess.boardId.equals(rs.getBoardId().intValue())
                            || !tenantAccess.employerId.equals(rs.getEmployerId().intValue())) {
                        throw new IllegalStateException("Request settlement tenant mismatch for voucher");
                    }
                    rs.setVoucherId(voucherHeader.getVoucherId().longValue());
                    rs.setUpdatedAt(now);
                    rs.setStatusId(statusService.requireStatusId(
                            "request_settlement",
                            rs.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "ALLOCATED"));
                    requestSettlementRepository.save(rs);
                });
    }

}
