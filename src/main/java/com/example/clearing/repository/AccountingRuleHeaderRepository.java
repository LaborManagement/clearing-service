package com.example.clearing.repository;

import com.example.clearing.domain.AccountingRuleHeader;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingRuleHeaderRepository extends JpaRepository<AccountingRuleHeader, Integer> {

    List<AccountingRuleHeader> findByEventTypeIdAndActiveTrueOrderByPriorityAsc(Integer eventTypeId);
}
