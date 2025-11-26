package com.example.clearing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.clearing.domain.AccountingEvent;
import com.example.clearing.domain.AccountingEventType;
import com.example.clearing.domain.AccountingRuleHeader;
import com.example.clearing.domain.AccountingRuleLine;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.domain.VoucherLine;
import com.example.clearing.dto.AllocationBreakdown;
import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.dto.SettlementResponse;
import com.example.clearing.repository.AccountingEventRepository;
import com.example.clearing.repository.AccountingEventTypeRepository;
import com.example.clearing.repository.AccountingRuleHeaderRepository;
import com.example.clearing.repository.AccountingRuleLineRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import com.example.clearing.repository.VoucherLineRepository;
import com.shared.common.dao.TenantAccessDao;

import jakarta.transaction.Transactional;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private static final String SETTLEMENT_EVENT_CODE = "EMP_REQ_SETTLED";
    private final AccountingEventTypeRepository eventTypeRepository;
    private final AccountingEventRepository eventRepository;
    private final AccountingRuleHeaderRepository ruleHeaderRepository;
    private final AccountingRuleLineRepository ruleLineRepository;
    private final VoucherHeaderRepository voucherHeaderRepository;
    private final VoucherLineRepository voucherLineRepository;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;

    public SettlementService(
            AccountingEventTypeRepository eventTypeRepository,
            AccountingEventRepository eventRepository,
            AccountingRuleHeaderRepository ruleHeaderRepository,
            AccountingRuleLineRepository ruleLineRepository,
            VoucherHeaderRepository voucherHeaderRepository,
            VoucherLineRepository voucherLineRepository,
            StatusService statusService,
            TenantAccessDao tenantAccessDao) {
        this.eventTypeRepository = eventTypeRepository;
        this.eventRepository = eventRepository;
        this.ruleHeaderRepository = ruleHeaderRepository;
        this.ruleLineRepository = ruleLineRepository;
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

        AccountingEvent event = eventRepository
                .findByEventTypeIdAndRequestIdAndBoardIdAndEmployerId(
                        eventType.getEventTypeId(), request.getRequestId(), tenantAccess.boardId,
                        tenantAccess.employerId)
                .orElseGet(() -> createEvent(eventType.getEventTypeId(), request, tenantAccess));

        VoucherHeader voucherHeader = voucherHeaderRepository
                .findFirstByBoardIdAndEmployerIdAndVoucherNumber(
                        tenantAccess.boardId, tenantAccess.employerId, request.getIdempotencyKey())
                .orElseGet(() -> createVoucherHeaderShell(event, request, tenantAccess));

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

        OffsetDateTime now = OffsetDateTime.now();
        List<VoucherLine> existingLines = voucherLineRepository.findByVoucherId(voucherHeader.getVoucherId());
        List<VoucherLine> newLines = buildVoucherLines(request, existingLines, now, tenantAccess);

        if (!newLines.isEmpty()) {
            for (VoucherLine line : newLines) {
                line.setVoucherId(voucherHeader.getVoucherId());
            }
            voucherLineRepository.saveAll(newLines);
        }

        List<VoucherLine> allLines = new ArrayList<>(existingLines);
        allLines.addAll(newLines);
        BigDecimal totalAmount = allLines.stream()
                .map(VoucherLine::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    private AccountingEvent createEvent(Integer eventTypeId, SettlementRequest request, TenantAccess tenantAccess) {
        AccountingEvent newEvent = new AccountingEvent();
        newEvent.setEventTypeId(eventTypeId);
        newEvent.setRequestId(request.getRequestId());
        newEvent.setBoardId(tenantAccess.boardId);
        newEvent.setEmployerId(tenantAccess.employerId);
        newEvent.setToliId(tenantAccess.toliId);
        newEvent.setEventDate(LocalDate.now());
        newEvent.setAmount(request.getTotalAmount());
        newEvent.setStatus("RECEIVED");
        newEvent.setStatusId(statusService.requireStatusId("accounting_event", "RECEIVED"));
        OffsetDateTime now = OffsetDateTime.now();
        newEvent.setCreatedAt(now);
        newEvent.setUpdatedAt(now);
        return eventRepository.save(newEvent);
    }

    private VoucherHeader createVoucherHeaderShell(AccountingEvent event, SettlementRequest request,
            TenantAccess tenantAccess) {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherHeader voucherHeader = new VoucherHeader();
        voucherHeader.setBoardId(tenantAccess.boardId);
        voucherHeader.setEmployerId(tenantAccess.employerId);
        voucherHeader.setToliId(tenantAccess.toliId);
        voucherHeader.setVoucherNumber(request.getIdempotencyKey());
        voucherHeader.setVoucherDate(LocalDate.now());
        voucherHeader.setTotalDebit(request.getTotalAmount());
        voucherHeader.setStatus("CREATED");
        voucherHeader.setStatusId(statusService.requireStatusId("voucher_header", "CREATED"));
        voucherHeader.setCreatedAt(now);
        voucherHeader.setUpdatedAt(now);
        return voucherHeaderRepository.save(voucherHeader);
    }

    private List<VoucherLine> buildVoucherLines(
            SettlementRequest request,
            List<VoucherLine> existingLines,
            OffsetDateTime createdAt,
            TenantAccess tenantAccess) {
        List<VoucherLine> newLines = new ArrayList<>();
        int nextLineNo = existingLines.size() + 1;
        for (AllocationBreakdown allocation : request.getAllocations()) {
            VoucherLine line = new VoucherLine();
            line.setLineNumber(nextLineNo++);
            line.setAmount(allocation.getAmount());
            line.setDescription("ALLOCATION " + allocation.getAllocationId());
            line.setBoardId(tenantAccess.boardId);
            line.setEmployerId(tenantAccess.employerId);
            line.setToliId(tenantAccess.toliId);
            line.setCreatedAt(createdAt);
            line.setUpdatedAt(createdAt);
            newLines.add(line);
        }
        return newLines;
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
}
