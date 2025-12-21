package com.example.clearing.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.clearing.dto.AllocationDetailsView;
import com.shared.utilities.logger.LoggerFactoryProvider;

/**
 * DAO for querying allocation details with joins across voucher_header,
 * payment_allocation, bank_transaction, and request_settlement tables
 */
@Repository
public class AllocationDetailsDao {

    private static final Logger log = LoggerFactoryProvider.getLogger(AllocationDetailsDao.class);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public AllocationDetailsDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /**
     * Search allocation details with pagination and filtering
     * 
     * @param startDate             Mandatory start date for filtering (on
     *                              p.receipt_date)
     * @param endDate               Mandatory end date for filtering (on
     *                              p.receipt_date)
     * @param employerReceiptNumber Optional filter on employer receipt number
     * @param voucherNumber         Optional filter on voucher number
     * @param voucherDateStart      Optional filter on voucher date start
     * @param voucherDateEnd        Optional filter on voucher date end
     * @param txnDateStart          Optional filter on transaction date start
     * @param txnDateEnd            Optional filter on transaction date end
     * @param boardId               Board ID from context
     * @param employerId            Employer ID from context
     * @param pageable              Pagination and sorting info
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
            Integer boardId,
            Integer employerId,
            Pageable pageable) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required for secure pagination");
        }

        // Base query
        String baseSelect = """
                SELECT
                    c.voucher_number,
                    c.voucher_date,
                    p.employer_receipt_number,
                    ep.worker_receipt_number,
                    p.receipt_date,
                    rs.total_amount AS receipt_amount,
                    p.bank_txn_id,
                    b.internal_ref,
                    b.txn_ref,
                    b.amount AS txn_amount,
                    p.allocated_amount AS allocated_amount_from_txn,
                    b.remaining_amount AS remaining_amount_of_txn,
                    b.txn_date
                FROM clearing.payment_allocation p
                LEFT JOIN clearing.voucher_header c ON c.voucher_id = p.voucher_id
                LEFT JOIN payment_flow.employer_payment_receipts ep ON ep.id = p.request_id
                JOIN clearing.bank_transaction b ON p.bank_txn_id = b.bank_txn_id
                JOIN clearing.request_settlement rs ON p.request_id = rs.request_id
                WHERE p.board_id = :boardId
                  AND p.employer_id = :employerId
                """;

        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("boardId", boardId);
        params.put("employerId", employerId);

        // Mandatory date range filter on receipt_date
        whereClause.append(" AND p.receipt_date BETWEEN :startDate AND :endDate");
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        // Optional filters
        if (employerReceiptNumber != null && !employerReceiptNumber.trim().isEmpty()) {
            whereClause.append(" AND p.employer_receipt_number = :employerReceiptNumber");
            params.put("employerReceiptNumber", employerReceiptNumber.trim());
        }

        if (voucherNumber != null && !voucherNumber.trim().isEmpty()) {
            whereClause.append(" AND c.voucher_number = :voucherNumber");
            params.put("voucherNumber", voucherNumber.trim());
        }

        if (voucherDateStart != null && voucherDateEnd != null) {
            whereClause.append(" AND c.voucher_date BETWEEN :voucherDateStart AND :voucherDateEnd");
            params.put("voucherDateStart", voucherDateStart);
            params.put("voucherDateEnd", voucherDateEnd);
        } else if (voucherDateStart != null) {
            whereClause.append(" AND c.voucher_date >= :voucherDateStart");
            params.put("voucherDateStart", voucherDateStart);
        } else if (voucherDateEnd != null) {
            whereClause.append(" AND c.voucher_date <= :voucherDateEnd");
            params.put("voucherDateEnd", voucherDateEnd);
        }

        if (txnDateStart != null && txnDateEnd != null) {
            whereClause.append(" AND b.txn_date BETWEEN :txnDateStart AND :txnDateEnd");
            params.put("txnDateStart", txnDateStart);
            params.put("txnDateEnd", txnDateEnd);
        } else if (txnDateStart != null) {
            whereClause.append(" AND b.txn_date >= :txnDateStart");
            params.put("txnDateStart", txnDateStart);
        } else if (txnDateEnd != null) {
            whereClause.append(" AND b.txn_date <= :txnDateEnd");
            params.put("txnDateEnd", txnDateEnd);
        }

        // Build ORDER BY clause from Pageable
        StringBuilder orderByClause = new StringBuilder(" ORDER BY ");
        if (pageable.getSort().isSorted()) {
            orderByClause.append(pageable.getSort().stream()
                    .map(order -> {
                        String column = mapSortField(order.getProperty());
                        return column + " " + (order.isAscending() ? "ASC" : "DESC");
                    })
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("p.receipt_date DESC"));
        } else {
            orderByClause.append("p.receipt_date DESC");
        }

        // Count query
        String countSql = "SELECT COUNT(*) FROM (" + baseSelect + whereClause.toString() + ") subquery";
        Long total = namedParameterJdbcTemplate.queryForObject(countSql, params, Long.class);
        if (total == null) {
            total = 0L;
        }

        // Data query with pagination
        String dataSql = baseSelect + whereClause.toString() + orderByClause.toString() +
                " LIMIT :limit OFFSET :offset";
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        log.debug("Executing allocation details search SQL: {} with params {}", dataSql, params);
        List<AllocationDetailsView> results = namedParameterJdbcTemplate.query(
                dataSql,
                params,
                new AllocationDetailsRowMapper());

        log.debug("Fetched {} allocation details out of {} total", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Map sort field names to database column names
     */
    private String mapSortField(String sortField) {
        return switch (sortField) {
            case "voucherNumber" -> "c.voucher_number";
            case "voucherDate" -> "c.voucher_date";
            case "employerReceiptNumber" -> "p.employer_receipt_number";
            case "workerReceiptNumber" -> "ep.worker_receipt_number";
            case "receiptDate" -> "p.receipt_date";
            case "receiptAmount" -> "rs.total_amount";
            case "txnDate" -> "b.txn_date";
            case "txnAmount", "amount" -> "b.amount";
            case "allocatedAmountFromTxn" -> "p.allocated_amount";
            case "createdAt" -> "p.created_at";
            case "id" -> "p.allocation_id";
            default -> "p.receipt_date"; // Default sort
        };
    }

    /**
     * RowMapper for AllocationDetailsView
     */
    private static class AllocationDetailsRowMapper implements RowMapper<AllocationDetailsView> {
        @Override
        @SuppressWarnings("null")
        public AllocationDetailsView mapRow(ResultSet rs, int rowNum) throws SQLException {
            AllocationDetailsView view = new AllocationDetailsView();
            view.setVoucherNumber(rs.getString("voucher_number"));

            java.sql.Date voucherDate = rs.getDate("voucher_date");
            view.setVoucherDate(voucherDate != null ? voucherDate.toLocalDate() : null);

            view.setEmployerReceiptNumber(rs.getString("employer_receipt_number"));
            view.setWorkerReceiptNumber(rs.getString("worker_receipt_number"));

            java.sql.Date receiptDate = rs.getDate("receipt_date");
            view.setReceiptDate(receiptDate != null ? receiptDate.toLocalDate() : null);

            view.setReceiptAmount(rs.getBigDecimal("receipt_amount"));
            view.setBankTxnId(rs.getLong("bank_txn_id"));
            view.setInternalRef(rs.getString("internal_ref"));
            view.setTxnRef(rs.getString("txn_ref"));
            view.setTxnAmount(rs.getBigDecimal("txn_amount"));
            view.setAllocatedAmountFromTxn(rs.getBigDecimal("allocated_amount_from_txn"));
            view.setRemainingAmountOfTxn(rs.getBigDecimal("remaining_amount_of_txn"));

            java.sql.Date txnDate = rs.getDate("txn_date");
            view.setTxnDate(txnDate != null ? txnDate.toLocalDate() : null);

            return view;
        }
    }
}
