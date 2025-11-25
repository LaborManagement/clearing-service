package com.example.clearing.repository;

import com.example.clearing.domain.AccountingEventType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingEventTypeRepository extends JpaRepository<AccountingEventType, Integer> {

    Optional<AccountingEventType> findByCode(String code);
}
