package com.example.clearing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.clearing.domain.AccountingEvent;
import com.example.clearing.domain.AccountingEventType;
import com.example.clearing.domain.AccountingRuleHeader;
import com.example.clearing.domain.AccountingRuleLine;
import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.domain.VoucherLine;
import com.example.clearing.dto.AllocationBreakdown;
import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.dto.SettlementResponse;
import com.example.clearing.repository.AccountingEventRepository;
import com.example.clearing.repository.AccountingEventTypeRepository;
import com.example.clearing.repository.AccountingRuleHeaderRepository;
import com.example.clearing.repository.AccountingRuleLineRepository;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import com.example.clearing.repository.VoucherLineRepository;
import com.shared.common.dao.TenantAccessDao;

import jakarta.transaction.Transactional;

@Service
public class SettlementService {

    private static final String SETTLEMENT_EVENT_CODE = "EMP_REQ_SETTLED";

    private final AccountingEventTypeRepository eventTypeRepository;
    private final AccountingEventRepository eventRepository;
    private final AccountingRuleHeaderRepository ruleHeaderRepository;
    private final AccountingRuleLineRepository ruleLineRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final VoucherHeaderRepository voucherHeaderRepository;
    private final VoucherLineRepository voucherLineRepository;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;

    public SettlementService(
            AccountingEventTypeRepository eventTypeRepository,
            AccountingEventRepository eventRepository,
            AccountingRuleHeaderRepository ruleHeaderRepository,
            AccountingRuleLineRepository ruleLineRepository,
            PaymentAllocationRepository paymentAllocationRepository,
            VoucherHeaderRepository voucherHeaderRepository,
            VoucherLineRepository voucherLineRepository,
            StatusService statusService,
            TenantAccessDao tenantAccessDao) {
        this.eventTypeRepository = eventTypeRepository;
        this.eventRepository = eventRepository;
        this.ruleHeaderRepository = ruleHeaderRepository;
        this.ruleLineRepository = ruleLineRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.voucherHeaderRepository = voucherHeaderRepository;
        this.voucherLineRepository = voucherLineRepository;
        this.statusService = statusService;
        this.tenantAccessDao = tenantAccessDao;
    }

    @Transactional
    public SettlementResponse processSettlement(SettlementRequest request) {
        AccountingEventType eventType = eventTypeRepository.findByCode(SETTLEMENT_EVENT_CODE)
                .orElseThrow(() -> new IllegalStateException("Missing accounting_event_type EMP_REQ_SETTLED"));

        TenantAccess tenantAccess = resolveTenantAccess(request);
        BigDecimal totalAmount = sumAllocations(request);
        if (request.getTotalAmount() != null && totalAmount.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Sum of allocations does not match totalAmount");
        }
        String voucherNumber = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : "REQ-" + request.getRequestId();

        AccountingEvent event = eventRepository
                .findByEventTypeIdAndRequestIdAndBoardIdAndEmployerId(
                        eventType.getEventTypeId(), request.getRequestId(), tenantAccess.boardId,
                        tenantAccess.employerId)
                .orElseGet(() -> createEvent(eventType.getEventTypeId(), totalAmount, request, tenantAccess));

        AccountingRuleHeader ruleHeader = ruleHeaderRepository
                .findByEventTypeIdAndActiveTrueOrderByPriorityAsc(eventType.getEventTypeId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active accounting_rule_header for event type " + eventType.getCode()));

        List<AccountingRuleLine> ruleLines = ruleLineRepository
                .findByRuleHeaderIdOrderByLineNoAsc(ruleHeader.getRuleHeaderId());
        if (ruleLines.isEmpty()) {
            throw new IllegalStateException(
                    "No accounting_rule_line for rule_header_id " + ruleHeader.getRuleHeaderId());
        }

        VoucherHeader voucherHeader = voucherHeaderRepository
                .findFirstByBoardIdAndEmployerIdAndVoucherNumber(
                        tenantAccess.boardId, tenantAccess.employerId, voucherNumber)
                .orElseGet(() -> createVoucherHeaderShell(voucherNumber, totalAmount, tenantAccess));

        OffsetDateTime now = OffsetDateTime.now();
        List<VoucherLine> existingLines = voucherLineRepository.findByVoucherId(voucherHeader.getVoucherId());
        List<VoucherLine> newLines = buildVoucherLinesFromRules(ruleLines, totalAmount, request, existingLines, now,
                tenantAccess);

        if (!newLines.isEmpty()) {
            for (VoucherLine line : newLines) {
                line.setVoucherId(voucherHeader.getVoucherId());
            }
            voucherLineRepository.saveAll(newLines);
        }

        linkVoucherToPaymentAllocations(voucherHeader, request, tenantAccess, now);

        voucherHeader.setTotalDebit(totalAmount);
        voucherHeader.setUpdatedAt(now);
        voucherHeader.setStatus("POSTED");
        voucherHeader.setStatusId(statusService.requireStatusId("voucher_header", "POSTED"));

        event.setStatus("PROCESSED");
        event.setStatusId(statusService.requireStatusId("accounting_event", "PROCESSED"));
        event.setUpdatedAt(now);

        voucherHeaderRepository.save(voucherHeader);
        eventRepository.save(event);

        return new SettlementResponse(
                event.getEventId(),
                voucherHeader.getVoucherId(),
                voucherHeader.getStatus(),
                voucherHeader.getTotalDebit(),
                voucherHeader.getTotalDebit(),
                "Voucher posted");
    }

    private AccountingEvent createEvent(
            Integer eventTypeId, BigDecimal totalAmount, SettlementRequest request, TenantAccess tenantAccess) {
        AccountingEvent newEvent = new AccountingEvent();
        newEvent.setEventTypeId(eventTypeId);
        newEvent.setRequestId(request.getRequestId());
        newEvent.setBoardId(tenantAccess.boardId);
        newEvent.setEmployerId(tenantAccess.employerId);
        newEvent.setToliId(tenantAccess.toliId);
        newEvent.setEventDate(LocalDate.now());
        newEvent.setAmount(totalAmount);
        newEvent.setStatus("RECEIVED");
        newEvent.setStatusId(statusService.requireStatusId("accounting_event", "RECEIVED"));
        OffsetDateTime now = OffsetDateTime.now();
        newEvent.setCreatedAt(now);
        newEvent.setUpdatedAt(now);
        return eventRepository.save(newEvent);
    }

    private VoucherHeader createVoucherHeaderShell(String voucherNumber, BigDecimal totalAmount,
            TenantAccess tenantAccess) {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherHeader voucherHeader = new VoucherHeader();
        voucherHeader.setBoardId(tenantAccess.boardId);
        voucherHeader.setEmployerId(tenantAccess.employerId);
        voucherHeader.setToliId(tenantAccess.toliId);
        voucherHeader.setVoucherNumber(voucherNumber);
        voucherHeader.setVoucherDate(LocalDate.now());
        voucherHeader.setTotalDebit(totalAmount);
        voucherHeader.setStatus("CREATED");
        voucherHeader.setStatusId(statusService.requireStatusId("voucher_header", "CREATED"));
        voucherHeader.setCreatedAt(now);
        voucherHeader.setUpdatedAt(now);
        return voucherHeaderRepository.save(voucherHeader);
    }

    private List<VoucherLine> buildVoucherLinesFromRules(
            List<AccountingRuleLine> ruleLines,
            BigDecimal eventTotal,
            SettlementRequest request,
            List<VoucherLine> existingLines,
            OffsetDateTime createdAt,
            TenantAccess tenantAccess) {
        List<VoucherLine> newLines = new ArrayList<>();
        int nextLineNo = existingLines.size() + 1;
        for (AccountingRuleLine ruleLine : ruleLines) {
            if ("EVENT_TOTAL".equalsIgnoreCase(ruleLine.getAmountSource())) {
                if (hasExistingLine(existingLines, ruleLine.getGlSourceType(), null)) {
                    continue;
                }
                VoucherLine line = baseLine(nextLineNo++, tenantAccess, createdAt);
                line.setAmount(eventTotal);
                line.setDescription(ruleLine.getDrCrFlag() + " " + ruleLine.getGlSourceType());
                newLines.add(line);
            } else if ("PER_ALLOCATION".equalsIgnoreCase(ruleLine.getAmountSource())) {
                for (AllocationBreakdown allocation : request.getAllocations()) {
                    if (hasExistingLine(existingLines, ruleLine.getGlSourceType(), allocation.getBankTxnId())) {
                        continue;
                    }
                    VoucherLine line = baseLine(nextLineNo++, tenantAccess, createdAt);
                    line.setAmount(allocation.getAmount());
                    line.setDescription(ruleLine.getDrCrFlag() + " " + ruleLine.getGlSourceType()
                            + " TXN " + allocation.getBankTxnId());
                    newLines.add(line);
                }
            } else {
                throw new IllegalStateException("Unsupported amount_source: " + ruleLine.getAmountSource());
            }
        }
        return newLines;
    }

    private VoucherLine baseLine(int lineNo, TenantAccess tenantAccess, OffsetDateTime ts) {
        VoucherLine line = new VoucherLine();
        line.setLineNumber(lineNo);
        line.setBoardId(tenantAccess.boardId);
        line.setEmployerId(tenantAccess.employerId);
        line.setToliId(tenantAccess.toliId);
        line.setCreatedAt(ts);
        line.setUpdatedAt(ts);
        return line;
    }

    private boolean hasExistingLine(List<VoucherLine> existingLines, String glSource, Long bankTxnId) {
        return existingLines.stream().anyMatch(line -> {
            String desc = line.getDescription();
            if (desc == null || !desc.contains(glSource)) {
                return false;
            }
            if (bankTxnId == null) {
                return true;
            }
            return desc.contains("TXN " + bankTxnId);
        });
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

    private void linkVoucherToPaymentAllocations(
            VoucherHeader voucherHeader, SettlementRequest request, TenantAccess tenantAccess, OffsetDateTime now) {
        Map<Long, BigDecimal> amountsByTxn = request.getAllocations().stream()
                .collect(Collectors.groupingBy(
                        AllocationBreakdown::getBankTxnId,
                        Collectors.reducing(BigDecimal.ZERO, AllocationBreakdown::getAmount, BigDecimal::add)));

        List<PaymentAllocation> allocations = paymentAllocationRepository.findByRequestId(request.getRequestId());
        if (allocations.isEmpty()) {
            throw new IllegalStateException("No payment_allocation rows found for request " + request.getRequestId());
        }

        for (Map.Entry<Long, BigDecimal> entry : amountsByTxn.entrySet()) {
            Long bankTxnId = entry.getKey();
            BigDecimal required = entry.getValue();
            List<PaymentAllocation> candidates = allocations.stream()
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
}
