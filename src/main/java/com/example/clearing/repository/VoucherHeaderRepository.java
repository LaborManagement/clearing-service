package com.example.clearing.repository;

import com.example.clearing.domain.VoucherHeader;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface VoucherHeaderRepository extends JpaRepository<VoucherHeader, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VoucherHeader> findFirstByBoardIdAndEmployerIdAndVoucherNumber(
            Integer boardId, Integer employerId, String voucherNumber);

    @Query("""
            SELECT v FROM VoucherHeader v
            WHERE (:boardId IS NULL OR v.boardId = :boardId)
              AND (:employerId IS NULL OR v.employerId = :employerId)
              AND (:voucherNumber IS NULL OR v.voucherNumber = :voucherNumber)
              AND (:statusId IS NULL OR v.statusId = :statusId)
            ORDER BY v.voucherId DESC
            """)
    List<VoucherHeader> search(Integer boardId, Integer employerId, String voucherNumber, Integer statusId, Pageable pageable);
}
