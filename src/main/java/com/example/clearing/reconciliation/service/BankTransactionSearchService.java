package com.example.clearing.reconciliation.service;

import com.example.clearing.reconciliation.dao.BankTransactionSearchDao;
import com.example.clearing.reconciliation.model.BankTransactionSearchCriteria;
import com.example.clearing.reconciliation.model.BankTransactionView;
import com.shared.utilities.logger.LoggerFactoryProvider;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BankTransactionSearchService {

    private static final Logger log = LoggerFactoryProvider.getLogger(BankTransactionSearchService.class);

    private final BankTransactionSearchDao dao;

    public BankTransactionSearchService(BankTransactionSearchDao dao) {
        this.dao = dao;
    }

    public List<BankTransactionView> search(LocalDate txnDate,
                                            BigDecimal amount,
                                            String drCrFlag,
                                            Long bankAccountId,
                                            String bankAccountNumber,
                                            String txnRef,
                                            Integer limit) {
        BankTransactionSearchCriteria criteria = new BankTransactionSearchCriteria();
        criteria.setTxnDate(txnDate);
        criteria.setAmount(amount);
        criteria.setDrCrFlag(drCrFlag);
        criteria.setBankAccountId(bankAccountId);
        criteria.setBankAccountNumber(bankAccountNumber);
        criteria.setTxnRef(txnRef);

        if (!criteria.hasAnyFilter()) {
            throw new IllegalArgumentException("At least one filter (txnDate, amount, drCrFlag, bankAccountId, bankAccountNmbr, txnRef) must be provided.");
        }

        log.info("Searching bank transactions with criteria txnDate={}, amount={}, drCrFlag={}, bankAccountId={}, bankAccountNumber={}, txnRef={}, limit={}",
                txnDate, amount, drCrFlag, bankAccountId, bankAccountNumber, txnRef, limit);
        return dao.search(criteria, limit);
    }
}
