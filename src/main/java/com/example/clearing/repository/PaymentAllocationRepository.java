package com.example.clearing.repository;

import com.example.clearing.domain.PaymentAllocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Integer> {

    List<PaymentAllocation> findByRequestId(Long requestId);

    Optional<PaymentAllocation> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentAllocation> findByRequestIdAndBankTxnId(Long requestId, Integer bankTxnId);

    List<PaymentAllocation> findByRequestIdAndVoucherIdIsNull(Long requestId);

    @Query("""
            SELECT p FROM PaymentAllocation p
            WHERE (:requestId IS NULL OR p.requestId = :requestId)
              AND (:bankTxnId IS NULL OR p.bankTxnId = :bankTxnId)
              AND (:voucherId IS NULL OR p.voucherId = :voucherId)
              AND (:statusId IS NULL OR p.statusId = :statusId)
            ORDER BY p.allocationId DESC
            """)
    List<PaymentAllocation> search(Long requestId, Integer bankTxnId, Integer voucherId, Integer statusId, Pageable pageable);
}
