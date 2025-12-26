package com.example.clearing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.clearing.client.PaymentFlowClient;
import com.example.clearing.domain.BankTransaction;
import com.example.clearing.domain.PaymentAllocation;
import com.example.clearing.domain.RequestSettlement;
import com.example.clearing.model.AllocationRequest;
import com.example.clearing.model.AllocationResponse;
import com.example.clearing.repository.BankTransactionRepository;
import com.example.clearing.repository.PaymentAllocationRepository;
import com.example.clearing.repository.RequestSettlementRepository;
import com.shared.common.dao.TenantAccessDao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private RequestSettlementRepository requestSettlementRepository;

    @Mock
    private StatusService statusService;

    @Mock
    private TenantAccessDao tenantAccessDao;

    @Mock
    private SettlementService settlementService;

    @Mock
    private PaymentFlowClient paymentFlowClient;

    private AllocationService allocationService;
    private final Map<Integer, BankTransaction> bankTxnStore = new HashMap<>();
    private final Map<Long, RequestSettlement> settlementStore = new HashMap<>();
    private final Map<String, PaymentAllocation> allocationPairs = new HashMap<>();
    private final AtomicInteger allocationCounter = new AtomicInteger();

    @BeforeEach
    void setUp() {
        bankTxnStore.clear();
        settlementStore.clear();
        allocationPairs.clear();
        allocationCounter.set(0);

        TenantAccessDao.TenantAccess tenantAccess = new TenantAccessDao.TenantAccess();
        tenantAccess.boardId = 10;
        tenantAccess.employerId = 20;
        tenantAccess.toliId = 30;
        when(tenantAccessDao.getFirstAccessibleTenant()).thenReturn(tenantAccess);

        when(statusService.requireStatusId("payment_allocation", "ALLOCATED")).thenReturn(1);
        when(statusService.requireStatusId("payment_allocation", "SETTLED")).thenReturn(2);
        when(statusService.requireStatusId("request_settlement", "ALLOCATED")).thenReturn(3);
        when(statusService.requireStatusId("request_settlement", "SETTLED")).thenReturn(4);
        lenient().when(statusService.resolveStatusCode(anyString(), anyInt())).thenReturn("ALLOCATED");

        lenient().when(bankTransactionRepository.findById(anyInt()))
                .thenAnswer(invocation -> Optional.ofNullable(bankTxnStore.get(invocation.getArgument(0))));
        lenient().when(bankTransactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(paymentAllocationRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        lenient().when(paymentAllocationRepository.findByRequestIdAndBankTxnId(anyLong(), anyInt()))
                .thenAnswer(invocation -> Optional.ofNullable(allocationPairs
                        .get(pairKey(invocation.getArgument(0), invocation.getArgument(1)))));
        lenient().when(paymentAllocationRepository.findByRequestIdAndVoucherIdIsNull(anyLong()))
                .thenReturn(List.of());
        lenient().when(paymentAllocationRepository.save(any())).thenAnswer(invocation -> {
            PaymentAllocation allocation = invocation.getArgument(0);
            allocation.setAllocationId(allocationCounter.incrementAndGet());
            allocationPairs.put(pairKey(allocation.getRequestId(), allocation.getBankTxnId()), allocation);
            return allocation;
        });

        lenient().when(requestSettlementRepository.findByRequestId(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(settlementStore.get(invocation.getArgument(0))));
        lenient().when(requestSettlementRepository.save(any())).thenAnswer(invocation -> {
            RequestSettlement saved = invocation.getArgument(0);
            settlementStore.put(saved.getRequestId(), saved);
            return saved;
        });

        allocationService = new AllocationService(
                bankTransactionRepository,
                paymentAllocationRepository,
                requestSettlementRepository,
                statusService,
                tenantAccessDao,
                settlementService,
                paymentFlowClient);
    }

    @Test
    void allowsMultipleRequestIdsToShareBankTransaction() {
        bankTxnStore.put(900, createBankTransaction(900, new BigDecimal("500")));

        List<AllocationResponse> responses = allocationService.createAllocations(List.of(
                createRequest(1L, 900, new BigDecimal("200"), new BigDecimal("100")),
                createRequest(2L, 900, new BigDecimal("180"), new BigDecimal("80"))));

        assertEquals(2, responses.size());

        BankTransaction updated = bankTxnStore.get(900);
        assertEquals(0, updated.getRemainingAmount().compareTo(new BigDecimal("320")));
        verify(paymentAllocationRepository, times(2)).save(any());
    }

    @Test
    void allowsSingleRequestAcrossMultipleBankTransactions() {
        bankTxnStore.put(111, createBankTransaction(111, new BigDecimal("300")));
        bankTxnStore.put(222, createBankTransaction(222, new BigDecimal("400")));

        allocationService.createAllocations(List.of(
                createRequest(500L, 111, new BigDecimal("300"), new BigDecimal("150")),
                createRequest(500L, 222, new BigDecimal("300"), new BigDecimal("100"))));

        assertEquals(0, bankTxnStore.get(111).getRemainingAmount().compareTo(new BigDecimal("150")));
        assertEquals(0, bankTxnStore.get(222).getRemainingAmount().compareTo(new BigDecimal("300")));

        RequestSettlement settlement = settlementStore.get(500L);
        assertNotNull(settlement);
        assertEquals(0, settlement.getRemainingAmount().compareTo(new BigDecimal("50")));
        verify(paymentAllocationRepository, times(2)).save(any());
    }

    @Test
    void rejectsDuplicateRequestAndBankCombination() {
        bankTxnStore.put(777, createBankTransaction(777, new BigDecimal("250")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> allocationService.createAllocations(List.of(
                        createRequest(800L, 777, new BigDecimal("400"), new BigDecimal("100")),
                        createRequest(800L, 777, new BigDecimal("400"), new BigDecimal("50")))));

        assertEquals("Allocation already exists for this request and bank transaction", ex.getMessage());
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayload() {
        PaymentAllocation existing = new PaymentAllocation();
        existing.setAllocationId(62);
        existing.setRequestId(1L);
        existing.setBankTxnId(4);
        existing.setAllocatedAmount(new BigDecimal("100"));
        when(paymentAllocationRepository.findByIdempotencyKey("dup-key"))
                .thenReturn(Optional.of(existing));

        AllocationRequest request = createRequest(2L, 4, new BigDecimal("200"), new BigDecimal("200"));
        request.setIdempotencyKey("dup-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> allocationService.createAllocation(request));

        assertEquals("Idempotency key already used for a different allocation payload", ex.getMessage());
    }

    @Test
    void rejectsWhenBankTransactionDoesNotHaveEnoughRemainingAmount() {
        bankTxnStore.put(321, createBankTransaction(321, new BigDecimal("25.00")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> allocationService.createAllocations(List.of(
                        createRequest(9001L, 321, new BigDecimal("25.00"), new BigDecimal("30.00")))));

        assertEquals(
                "Insufficient funds on bank transaction 321: remaining 25.00, requested 30.00",
                ex.getMessage());
    }

    @Test
    void updatesPaymentFlowStatusToPartialWhenRemainingAmountExists() {
        bankTxnStore.put(123, createBankTransaction(123, new BigDecimal("500")));

        allocationService.createAllocations(List.of(
                createRequest(1L, 123, new BigDecimal("500"), new BigDecimal("100"))));

        verify(paymentFlowClient, times(1)).updatePaymentStatusById(1L, 4L);
    }

    @Test
    void updatesPaymentFlowStatusToReconciledWhenFullyAllocated() {
        bankTxnStore.put(124, createBankTransaction(124, new BigDecimal("150")));

        allocationService.createAllocations(List.of(
                createRequest(2L, 124, new BigDecimal("150"), new BigDecimal("150"))));

        verify(paymentFlowClient, times(1)).updatePaymentStatusById(2L, 5L);
    }

    private AllocationRequest createRequest(Long requestId, Integer bankTxnId, BigDecimal requestedAmount,
            BigDecimal allocatedAmount) {
        AllocationRequest request = new AllocationRequest();
        request.setRequestId(requestId);
        request.setBankTxnId(bankTxnId);
        request.setRequestedAmount(requestedAmount);
        request.setAllocatedAmount(allocatedAmount);
        request.setAllocationDate(LocalDate.now());
        request.setAllocatedBy("tester");
        return request;
    }

    private BankTransaction createBankTransaction(Integer id, BigDecimal remaining) {
        BankTransaction txn = new BankTransaction();
        txn.setBankTxnId(id);
        txn.setBankAccountId(5);
        txn.setAmount(new BigDecimal("1000"));
        txn.setDrCrFlag("CR");
        txn.setBoardId(10);
        txn.setEmployerId(20);
        txn.setToliId(30);
        txn.setRemainingAmount(remaining);
        txn.setAllocatedAmount(BigDecimal.ZERO);
        return txn;
    }

    private String pairKey(Long requestId, Integer bankTxnId) {
        return requestId + "::" + bankTxnId;
    }
}
