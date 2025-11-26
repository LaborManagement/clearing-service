package com.example.clearing.repository;

import com.example.clearing.domain.VoucherHeader;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VoucherHeaderRepository extends JpaRepository<VoucherHeader, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VoucherHeader> findFirstByBoardIdAndEmployerIdAndVoucherNumber(
            Integer boardId, Integer employerId, String voucherNumber);
}
