package com.example.clearing.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.clearing.domain.BankTransaction;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

        Optional<BankTransaction> findBySourceSystemAndSourceTxnId(String sourceSystem, String sourceTxnId);

        @Query(value = """
                        SELECT * FROM clearing.bank_transaction bt
                        WHERE bt.board_id = :boardId
                          AND bt.employer_id = :employerId
                          AND (CAST(:bankTxnId AS INTEGER) IS NULL OR bt.bank_txn_id = CAST(:bankTxnId AS INTEGER))
                          AND (CAST(:txnRef AS VARCHAR) IS NULL OR bt.txn_ref = CAST(:txnRef AS VARCHAR))
                          AND (CAST(:isSettled AS BOOLEAN) IS NULL OR bt.is_settled = CAST(:isSettled AS BOOLEAN))
                          AND (CAST(:startDate AS DATE) IS NULL OR CAST(bt.created_at AS DATE) >= CAST(:startDate AS DATE))
                          AND (CAST(:endDate AS DATE) IS NULL OR CAST(bt.created_at AS DATE) <= CAST(:endDate AS DATE))
                        ORDER BY bt.updated_at DESC
                        """, nativeQuery = true)
        Page<BankTransaction> findByFilters(
                        @Param("boardId") Integer boardId,
                        @Param("employerId") Integer employerId,
                        @Param("bankTxnId") Integer bankTxnId,
                        @Param("txnRef") String txnRef,
                        @Param("isSettled") Boolean isSettled,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);
}
