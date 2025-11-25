package com.example.clearing.repository;

import com.example.clearing.domain.VoucherHeader;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherHeaderRepository extends JpaRepository<VoucherHeader, Integer> {

    Optional<VoucherHeader> findFirstByEventId(Integer eventId);
}
