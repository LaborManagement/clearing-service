package com.example.clearing.repository;

import com.example.clearing.domain.BankTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

    Optional<BankTransaction> findBySourceSystemAndSourceTxnId(String sourceSystem, String sourceTxnId);
}
