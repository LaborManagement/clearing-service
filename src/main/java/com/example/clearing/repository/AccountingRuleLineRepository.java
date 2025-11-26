package com.example.clearing.repository;

import com.example.clearing.domain.AccountingRuleLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingRuleLineRepository extends JpaRepository<AccountingRuleLine, Integer> {

    List<AccountingRuleLine> findByRuleHeaderIdOrderByLineNoAsc(Integer ruleHeaderId);
}
