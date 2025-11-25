package com.example.clearing.repository;

import com.example.clearing.domain.VoucherLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherLineRepository extends JpaRepository<VoucherLine, Integer> {

    List<VoucherLine> findByVoucherId(Integer voucherId);
}
