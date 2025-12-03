package com.example.clearing.service;

import com.example.clearing.dao.BankTransactionSearchDao;
import com.example.clearing.domain.BankTransaction;
import com.example.clearing.model.BankTransactionSearchCriteria;
import com.example.clearing.model.BankTransactionView;
import com.example.clearing.repository.BankTransactionRepository;
import com.shared.common.dao.TenantAccessDao;
import com.shared.utilities.logger.LoggerFactoryProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankTransactionSearchService {

    private static final Logger log = LoggerFactoryProvider.getLogger(BankTransactionSearchService.class);

    private final BankTransactionSearchDao dao;
    private final BankTransactionRepository bankTransactionRepository;
    private final TenantAccessDao tenantAccessDao;
    private final StatusService statusService;

    public BankTransactionSearchService(
            BankTransactionSearchDao dao,
            BankTransactionRepository bankTransactionRepository,
            TenantAccessDao tenantAccessDao,
            StatusService statusService) {
        this.dao = dao;
        this.bankTransactionRepository = bankTransactionRepository;
        this.tenantAccessDao = tenantAccessDao;
        this.statusService = statusService;
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
            throw new IllegalArgumentException(
                    "At least one filter (txnDate, amount, drCrFlag, bankAccountId, bankAccountNmbr, txnRef) must be provided.");
        }

        log.info(
                "Searching bank transactions with criteria txnDate={}, amount={}, drCrFlag={}, bankAccountId={}, bankAccountNumber={}, txnRef={}, limit={}",
                txnDate, amount, drCrFlag, bankAccountId, bankAccountNumber, txnRef, limit);
        return dao.search(criteria, limit);
    }

    public List<BankTransaction> findClearingTransactions(
            Integer bankTxnId, String txnRef, Boolean isSettled, int limit) {
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) to list bank transactions");
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        log.info("Fetching clearing bank transactions bankTxnId={}, txnRef={}, isSettled={}, boardId={}, employerId={}",
                bankTxnId, txnRef, isSettled, ta.boardId, ta.employerId);
        List<BankTransaction> txns = bankTransactionRepository.findByFilters(
                ta.boardId, ta.employerId, bankTxnId, txnRef, isSettled, pageable);
        txns.forEach(txn -> txn.setStatusCode(statusService.resolveStatusCode("bank_transaction", txn.getStatusId())));
        return txns;
    }

    public Page<BankTransactionView> searchSecure(LocalDate startDate,
            LocalDate endDate,
            BigDecimal amount,
            String drCrFlag,
            Long bankAccountId,
            String bankAccountNumber,
            String txnRef,
            String statusCode,
            Pageable pageable) {
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) to list bank transactions");
        }
        BankTransactionSearchCriteria criteria = new BankTransactionSearchCriteria();
        criteria.setAmount(amount);
        criteria.setDrCrFlag(drCrFlag);
        criteria.setBankAccountId(bankAccountId);
        criteria.setBankAccountNumber(bankAccountNumber);
        criteria.setTxnRef(txnRef);
        Integer resolvedStatusId = statusCode != null && !statusCode.isBlank()
                ? statusService.requireStatusId("bank_transaction", statusCode.trim())
                : null;
        criteria.setStatusId(resolvedStatusId);

        log.info(
                "Secure paginated search for bank transactions startDate={}, endDate={}, amount={}, drCrFlag={}, bankAccountId={}, bankAccountNumber={}, txnRef={}, statusId={}, page={}, size={}",
                startDate, endDate, amount, drCrFlag, bankAccountId, bankAccountNumber, txnRef, resolvedStatusId,
                pageable != null ? pageable.getPageNumber() : null,
                pageable != null ? pageable.getPageSize() : null);
        Page<BankTransactionView> result = dao.searchPaginated(
                criteria, startDate, endDate, ta.boardId, ta.employerId, pageable);
        result.forEach(v -> {
            if (v.getStatusId() != null) {
                v.setStatus(statusService.resolveStatusCode("bank_transaction", v.getStatusId()));
            }
        });
        return result;
    }
}
