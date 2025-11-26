package com.example.clearing.repository;

import com.example.clearing.domain.AccountingEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingEventRepository extends JpaRepository<AccountingEvent, Integer> {

    Optional<AccountingEvent> findByEventTypeIdAndRequestIdAndBoardIdAndEmployerId(
            Integer eventTypeId, Long requestId, Integer boardId, Integer employerId);
}
