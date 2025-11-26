package com.example.clearing.repository;

import com.example.clearing.domain.PaymentAllocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Integer> {

    List<PaymentAllocation> findByRequestId(Long requestId);

    Optional<PaymentAllocation> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentAllocation> findByRequestIdAndBankTxnId(Long requestId, Integer bankTxnId);
}
