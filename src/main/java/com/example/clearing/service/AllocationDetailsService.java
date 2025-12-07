package com.example.clearing.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clearing.dao.AllocationDetailsDao;
import com.example.clearing.dto.AllocationDetailsView;
import com.shared.common.dao.TenantAccessDao;
import com.shared.utilities.logger.LoggerFactoryProvider;

/**
 * Service for querying allocation details across voucher, payment allocation,
 * bank transaction and request settlement tables
 */
@Service
@Transactional(readOnly = true)
public class AllocationDetailsService {

    private static final Logger log = LoggerFactoryProvider.getLogger(AllocationDetailsService.class);

    private final AllocationDetailsDao dao;
    private final TenantAccessDao tenantAccessDao;

    public AllocationDetailsService(AllocationDetailsDao dao, TenantAccessDao tenantAccessDao) {
        this.dao = dao;
        this.tenantAccessDao = tenantAccessDao;
    }

    /**
     * Search allocation details with secure pagination and filtering
     * 
     * @param startDate             Mandatory start date for filtering (on
     *                              receipt_date)
     * @param endDate               Mandatory end date for filtering (on
     *                              receipt_date)
     * @param employerReceiptNumber Optional filter
     * @param voucherNumber         Optional filter
     * @param voucherDateStart      Optional filter
     * @param voucherDateEnd        Optional filter
     * @param txnDateStart          Optional filter
     * @param txnDateEnd            Optional filter
     * @param pageable              Pagination and sorting
     * @return Page of AllocationDetailsView
     */
    public Page<AllocationDetailsView> searchAllocationDetails(
            LocalDate startDate,
            LocalDate endDate,
            String employerReceiptNumber,
            String voucherNumber,
            LocalDate voucherDateStart,
            LocalDate voucherDateEnd,
            LocalDate txnDateStart,
            LocalDate txnDateEnd,
            Pageable pageable) {

        // Get tenant context
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) to query allocation details");
        }

        log.info("Searching allocation details: startDate={}, endDate={}, employerReceiptNumber={}, " +
                "voucherNumber={}, voucherDateRange=[{},{}], txnDateRange=[{},{}], boardId={}, employerId={}, page={}, size={}",
                startDate, endDate, employerReceiptNumber, voucherNumber,
                voucherDateStart, voucherDateEnd, txnDateStart, txnDateEnd,
                ta.boardId, ta.employerId, pageable.getPageNumber(), pageable.getPageSize());

        return dao.searchAllocationDetails(
                startDate, endDate, employerReceiptNumber, voucherNumber,
                voucherDateStart, voucherDateEnd, txnDateStart, txnDateEnd,
                ta.boardId, ta.employerId, pageable);
    }
}
