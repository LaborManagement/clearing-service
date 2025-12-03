package com.example.clearing.repository;

import com.example.clearing.domain.DrcrNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrcrNoteRepository extends JpaRepository<DrcrNote, Long> {

    Optional<DrcrNote> findByIdAndBoardIdAndEmployerId(Long id, Integer boardId, Integer employerId);

    @Query("""
            SELECT d FROM DrcrNote d
            WHERE d.boardId = :boardId
              AND d.employerId = :employerId
              AND (:requestId IS NULL OR d.requestId = :requestId)
              AND (:voucherType IS NULL OR LOWER(d.voucherType) = LOWER(:voucherType))
            ORDER BY d.updatedAt DESC, d.id DESC
            """)
    List<DrcrNote> search(
            @Param("boardId") Integer boardId,
            @Param("employerId") Integer employerId,
            @Param("requestId") Long requestId,
            @Param("voucherType") String voucherType,
            Pageable pageable);
}
