package com.example.clearing.repository;

import com.example.clearing.domain.RequestSettlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequestSettlementRepository extends JpaRepository<RequestSettlement, Long> {

    Optional<RequestSettlement> findByRequestId(Long requestId);

    @Query("""
            SELECT r FROM RequestSettlement r
            WHERE (:requestId IS NULL OR r.requestId = :requestId)
              AND (:boardId IS NULL OR r.boardId = :boardId)
              AND (:employerId IS NULL OR r.employerId = :employerId)
              AND (:statusId IS NULL OR r.statusId = :statusId)
            ORDER BY r.requestSettlementId DESC
            """)
    List<RequestSettlement> search(Long requestId, Long boardId, Long employerId, Integer statusId, Pageable pageable);
}
