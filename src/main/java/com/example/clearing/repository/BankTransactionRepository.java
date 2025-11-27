package com.example.clearing.repository;

import com.example.clearing.domain.BankTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

    Optional<BankTransaction> findBySourceSystemAndSourceTxnId(String sourceSystem, String sourceTxnId);

    @Query("""
            SELECT b FROM BankTransaction b
            WHERE b.boardId = :boardId
              AND b.employerId = :employerId
              AND (:bankTxnId IS NULL OR b.bankTxnId = :bankTxnId)
              AND (:txnRef IS NULL OR b.txnRef = :txnRef)
              AND (:isSettled IS NULL OR b.isSettled = :isSettled)
            ORDER BY b.updatedAt DESC
            """)
    List<BankTransaction> findByFilters(
            @Param("boardId") Integer boardId,
            @Param("employerId") Integer employerId,
            @Param("bankTxnId") Integer bankTxnId,
            @Param("txnRef") String txnRef,
            @Param("isSettled") Boolean isSettled,
            Pageable pageable);
}
