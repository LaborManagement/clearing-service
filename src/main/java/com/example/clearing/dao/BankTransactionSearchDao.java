package com.example.clearing.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.clearing.common.sql.SqlTemplateLoader;
import com.example.clearing.model.BankTransactionSearchCriteria;
import com.example.clearing.model.BankTransactionView;
import com.shared.utilities.logger.LoggerFactoryProvider;

@Repository
public class BankTransactionSearchDao {

    private static final Logger log = LoggerFactoryProvider.getLogger(BankTransactionSearchDao.class);
    private static final String BASE_SELECT_TEMPLATE = "sql/reconciliation/bank_transactions_base_select.sql";
    private static final Map<String, String> SORT_COLUMN_MAP_VIEW = Map.of(
            "receiptDate", "v.txn_date",
            "createdAt", "v.created_at",
            "amount", "v.amount",
            "id", "v.source_txn_id");
    private static final Map<String, String> SORT_COLUMN_MAP_BANK_TXN = Map.of(
            "receiptDate", "bt.txn_date",
            "createdAt", "bt.created_at",
            "amount", "bt.amount",
            "id", "bt.source_txn_id");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SqlTemplateLoader sqlTemplates;

    public BankTransactionSearchDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            SqlTemplateLoader sqlTemplates) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.sqlTemplates = sqlTemplates;
    }

    public List<BankTransactionView> search(BankTransactionSearchCriteria criteria, Integer limit) {
        String baseSql = sqlTemplates.load(BASE_SELECT_TEMPLATE);
        StringBuilder sql = new StringBuilder(baseSql);
        Map<String, Object> params = new HashMap<>();

        if (criteria.getTxnDate() != null) {
            sql.append(" AND v.txn_date = :txnDate");
            params.put("txnDate", criteria.getTxnDate());
        }
        if (criteria.getAmount() != null) {
            sql.append(" AND v.amount = :amount");
            params.put("amount", criteria.getAmount());
        }
        if (hasText(criteria.getDrCrFlag())) {
            sql.append(" AND UPPER(v.dr_cr_flag) = :drCrFlag");
            params.put("drCrFlag", criteria.getDrCrFlag().trim().toUpperCase());
        }
        if (criteria.getBankAccountId() != null) {
            sql.append(" AND v.bank_account_id = :bankAccountId");
            params.put("bankAccountId", criteria.getBankAccountId());
        }
        if (hasText(criteria.getBankAccountNumber())) {
            sql.append(" AND ba.account_no = :bankAccountNumber");
            params.put("bankAccountNumber", criteria.getBankAccountNumber().trim());
        }
        if (hasText(criteria.getTxnRef())) {
            sql.append(" AND v.txn_ref = :txnRef");
            params.put("txnRef", criteria.getTxnRef().trim());
        }

        sql.append(" ORDER BY v.txn_date DESC, v.created_at DESC");
        if (limit != null && limit > 0) {
            sql.append(" LIMIT :limit");
            params.put("limit", limit);
        }

        log.debug("Executing bank transaction search SQL: {} with params {}", sql, params);
        List<BankTransactionView> results = namedParameterJdbcTemplate.query(
                sql.toString(),
                params,
                new BankTransactionRowMapper());
        log.debug("Fetched {} transactions for criteria {}", results.size(), criteria);
        return results;
    }

    public Page<BankTransactionView> searchPaginated(BankTransactionSearchCriteria criteria,
            LocalDate startDate, LocalDate endDate, Integer boardId, Integer employerId, Pageable pageable) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required for secure pagination");
        }

        String baseSql = """
                SELECT
                    bt.txn_type AS type,
                    bt.source_system,
                    bt.source_txn_id,
                bt.bank_account_id,
                ba.account_no AS bank_account_number,
                bt.txn_ref,
                bt.internal_ref,
                bt.txn_date,
                bt.amount,
                bt.allocated_amount,
                bt.remaining_amount,
                bt.dr_cr_flag,
                bt.description,
                NULL::boolean AS is_mapped,
                bt.created_at,
                bt.status_id
                FROM clearing.bank_transaction bt
                LEFT JOIN reconciliation.bank_account ba ON ba.id = bt.bank_account_id
                WHERE bt.board_id = :boardId
                  AND bt.employer_id = :employerId
                """;
        StringBuilder filters = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        filters.append(" AND bt.created_at::date BETWEEN :startDate AND :endDate");
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("boardId", boardId);
        params.put("employerId", employerId);

        if (criteria.getTxnDate() != null) {
            filters.append(" AND bt.txn_date = :txnDate");
            params.put("txnDate", criteria.getTxnDate());
        }
        if (criteria.getAmount() != null) {
            filters.append(" AND bt.amount = :amount");
            params.put("amount", criteria.getAmount());
        }
        if (hasText(criteria.getDrCrFlag())) {
            filters.append(" AND UPPER(bt.dr_cr_flag) = :drCrFlag");
            params.put("drCrFlag", criteria.getDrCrFlag().trim().toUpperCase());
        }
        if (criteria.getBankAccountId() != null) {
            filters.append(" AND bt.bank_account_id = :bankAccountId");
            params.put("bankAccountId", criteria.getBankAccountId());
        }
        if (hasText(criteria.getBankAccountNumber())) {
            filters.append(" AND ba.account_no = :bankAccountNumber");
            params.put("bankAccountNumber", criteria.getBankAccountNumber().trim());
        }
        if (hasText(criteria.getTxnRef())) {
            filters.append(" AND bt.txn_ref = :txnRef");
            params.put("txnRef", criteria.getTxnRef().trim());
        }
        if (criteria.getStatusId() != null) {
            filters.append(" AND bt.status_id = :statusId");
            params.put("statusId", criteria.getStatusId());
        }

        Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
        StringBuilder sql = new StringBuilder(baseSql).append(filters);
        appendOrderBy(sql, sort, SORT_COLUMN_MAP_BANK_TXN);

        if (pageable != null) {
            sql.append(" LIMIT :limit OFFSET :offset");
            params.put("limit", pageable.getPageSize());
            params.put("offset", (long) pageable.getPageNumber() * pageable.getPageSize());
        }

        String countSql = "SELECT COUNT(*) FROM (" + baseSql + filters + ") AS count_base";

        log.debug("Executing paginated bank transaction search SQL: {} with params {}", sql, params);
        List<BankTransactionView> results = namedParameterJdbcTemplate.query(
                sql.toString(),
                params,
                new BankTransactionRowMapper());
        long total = namedParameterJdbcTemplate.queryForObject(countSql, params, Long.class);
        return new PageImpl<>(results, pageable, total);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void appendOrderBy(StringBuilder sql, Sort sort, Map<String, String> sortColumnMap) {
        if (sort == null || sort.isUnsorted()) {
            sql.append(" ORDER BY txn_date DESC, created_at DESC");
            return;
        }
        sql.append(" ORDER BY ");
        boolean first = true;
        for (Sort.Order order : sort) {
            if (!first) {
                sql.append(", ");
            }
            String column = sortColumnMap.getOrDefault(order.getProperty(), "created_at");
            sql.append(column).append(order.isAscending() ? " ASC" : " DESC");
            first = false;
        }
    }

    private static class BankTransactionRowMapper implements RowMapper<BankTransactionView> {
        @Override
        public BankTransactionView mapRow(ResultSet rs, int rowNum) throws SQLException {
            BankTransactionView view = new BankTransactionView();
            view.setType(rs.getString("type"));
            view.setSourceTxnId(rs.getString("source_txn_id"));
            long bankAccountId = rs.getLong("bank_account_id");
            if (!rs.wasNull()) {
                view.setBankAccountId(bankAccountId);
            }
            view.setBankAccountNumber(rs.getString("bank_account_number"));
            view.setTxnRef(rs.getString("txn_ref"));
            if (hasColumn(rs, "internal_ref")) {
                view.setInternalRef(rs.getString("internal_ref"));
            }

            java.sql.Date txnDate = rs.getDate("txn_date");
            if (txnDate != null) {
                view.setTxnDate(txnDate.toLocalDate());
            }

            view.setAmount(rs.getBigDecimal("amount"));
            if (hasColumn(rs, "allocated_amount")) {
                view.setAllocatedAmount(rs.getBigDecimal("allocated_amount"));
            }
            if (hasColumn(rs, "remaining_amount")) {
                view.setRemainingAmount(rs.getBigDecimal("remaining_amount"));
            }
            view.setDrCrFlag(rs.getString("dr_cr_flag"));
            view.setDescription(rs.getString("description"));
            view.setMapped(rs.getObject("is_mapped", Boolean.class));

            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                view.setCreatedAt(createdAt.toLocalDateTime());
            }
            Integer statusId = rs.getObject("status_id", Integer.class);
            if (statusId != null) {
                view.setStatusId(statusId);
            }
            return view;
        }

        private boolean hasColumn(ResultSet rs, String columnLabel) {
            try {
                return rs.findColumn(columnLabel) > 0;
            } catch (SQLException ex) {
                return false;
            }
        }
    }
}
