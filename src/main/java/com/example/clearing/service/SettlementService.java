package com.example.clearing.service;

import com.example.clearing.domain.AccountingEvent;
import com.example.clearing.domain.AccountingEventType;
import com.example.clearing.domain.VoucherHeader;
import com.example.clearing.domain.VoucherLine;
import com.example.clearing.dto.AllocationBreakdown;
import com.example.clearing.dto.SettlementRequest;
import com.example.clearing.dto.SettlementResponse;
import com.example.clearing.repository.AccountingEventRepository;
import com.example.clearing.repository.AccountingEventTypeRepository;
import com.example.clearing.repository.VoucherHeaderRepository;
import com.example.clearing.repository.VoucherLineRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private static final String SETTLEMENT_EVENT_CODE = "EMP_REQ_SETTLED";
    private static final String DR_GL_SOURCE = "EMPLOYEE_PAYABLE";
    private static final String CR_GL_SOURCE = "BANK_CLEARING";

    private final AccountingEventTypeRepository eventTypeRepository;
    private final AccountingEventRepository eventRepository;
    private final VoucherHeaderRepository voucherHeaderRepository;
    private final VoucherLineRepository voucherLineRepository;
    private final ObjectMapper objectMapper;

    public SettlementService(
            AccountingEventTypeRepository eventTypeRepository,
            AccountingEventRepository eventRepository,
            VoucherHeaderRepository voucherHeaderRepository,
            VoucherLineRepository voucherLineRepository,
            ObjectMapper objectMapper) {
        this.eventTypeRepository = eventTypeRepository;
        this.eventRepository = eventRepository;
        this.voucherHeaderRepository = voucherHeaderRepository;
        this.voucherLineRepository = voucherLineRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SettlementResponse processSettlement(SettlementRequest request) {
        AccountingEventType eventType = eventTypeRepository.findByCode(SETTLEMENT_EVENT_CODE)
                .orElseThrow(() -> new IllegalStateException("Missing accounting_event_type EMP_REQ_SETTLED"));

        String idempotencyKey = resolveIdempotencyKey(request);

        Optional<AccountingEvent> existing = eventRepository.findByEventTypeIdAndRequestIdAndIdempotencyKey(
                eventType.getEventTypeId(), request.getRequestId(), idempotencyKey);
        if (existing.isPresent()) {
            AccountingEvent event = existing.get();
            Optional<VoucherHeader> existingVoucher = voucherHeaderRepository.findFirstByEventId(event.getEventId());
            if (existingVoucher.isPresent()) {
                VoucherHeader voucher = existingVoucher.get();
                return new SettlementResponse(
                        voucher.getEventId(),
                        voucher.getVoucherId(),
                        voucher.getStatus(),
                        voucher.getTotalDebit(),
                        voucher.getTotalCredit(),
                        "Idempotent request - returning existing voucher");
            }
            return new SettlementResponse(
                    event.getEventId(), null, null, null, null, "Idempotent request - event already exists");
        }

        AccountingEvent newEvent = new AccountingEvent();
        newEvent.setEventTypeId(eventType.getEventTypeId());
        newEvent.setRequestId(request.getRequestId());
        newEvent.setIdempotencyKey(idempotencyKey);
        newEvent.setStatus("RECEIVED");
        newEvent.setPayload(toJsonQuietly(request));
        newEvent.setCreatedAt(OffsetDateTime.now());
        newEvent = eventRepository.save(newEvent);

        BigDecimal totalDebit = request.getTotalAmount();
        BigDecimal totalCredit = sumAllocations(request.getAllocations());

        VoucherHeader voucherHeader = new VoucherHeader();
        voucherHeader.setEventId(newEvent.getEventId());
        voucherHeader.setRequestId(request.getRequestId());
        voucherHeader.setBoardId(request.getBoardId());
        voucherHeader.setEmployerId(request.getEmployerId());
        voucherHeader.setTotalDebit(totalDebit);
        voucherHeader.setTotalCredit(totalCredit);
        voucherHeader.setStatus("POSTED");
        voucherHeader.setPostedAt(OffsetDateTime.now());
        voucherHeader.setCreatedAt(OffsetDateTime.now());
        voucherHeader.setUpdatedAt(OffsetDateTime.now());
        voucherHeader = voucherHeaderRepository.save(voucherHeader);

        List<VoucherLine> lines = new ArrayList<>();

        VoucherLine debitLine = new VoucherLine();
        debitLine.setVoucherId(voucherHeader.getVoucherId());
        debitLine.setDrCrFlag("DR");
        debitLine.setGlSourceType(DR_GL_SOURCE);
        debitLine.setAmount(totalDebit);
        debitLine.setDimensionSource("REQUEST");
        debitLine.setCreatedAt(OffsetDateTime.now());
        lines.add(debitLine);

        for (AllocationBreakdown allocation : request.getAllocations()) {
            VoucherLine creditLine = new VoucherLine();
            creditLine.setVoucherId(voucherHeader.getVoucherId());
            creditLine.setDrCrFlag("CR");
            creditLine.setGlSourceType(CR_GL_SOURCE);
            creditLine.setAmount(allocation.getAmount());
            creditLine.setBankTxnId(allocation.getBankTxnId());
            creditLine.setAllocationId(allocation.getAllocationId());
            creditLine.setDimensionSource("BANK_TXN");
            creditLine.setCreatedAt(OffsetDateTime.now());
            lines.add(creditLine);
        }

        voucherLineRepository.saveAll(lines);

        newEvent.setStatus("PROCESSED");
        newEvent.setProcessedAt(OffsetDateTime.now());
        eventRepository.save(newEvent);

        return new SettlementResponse(
                voucherHeader.getEventId(),
                voucherHeader.getVoucherId(),
                voucherHeader.getStatus(),
                voucherHeader.getTotalDebit(),
                voucherHeader.getTotalCredit(),
                "Voucher created");
    }

    private String resolveIdempotencyKey(SettlementRequest request) {
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            return request.getIdempotencyKey();
        }
        return "REQ-" + request.getRequestId();
    }

    private BigDecimal sumAllocations(List<AllocationBreakdown> allocations) {
        return allocations.stream()
                .map(AllocationBreakdown::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String toJsonQuietly(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload for accounting_event", e);
            return null;
        }
    }
}
