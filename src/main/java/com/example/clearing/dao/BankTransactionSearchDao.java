package com.example.clearing.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class BankTransactionRowMapper implements RowMapper<BankTransactionView> {
        @Override
        public BankTransactionView mapRow(ResultSet rs, int rowNum) throws SQLException {
            BankTransactionView view = new BankTransactionView();
            view.setType(rs.getString("type"));
            long bankAccountId = rs.getLong("bank_account_id");
            if (!rs.wasNull()) {
                view.setBankAccountId(bankAccountId);
            }
            view.setBankAccountNumber(rs.getString("bank_account_number"));
            view.setTxnRef(rs.getString("txn_ref"));

            java.sql.Date txnDate = rs.getDate("txn_date");
            if (txnDate != null) {
                view.setTxnDate(txnDate.toLocalDate());
            }

            view.setAmount(rs.getBigDecimal("amount"));
            view.setDrCrFlag(rs.getString("dr_cr_flag"));
            view.setDescription(rs.getString("description"));
            view.setMapped(rs.getObject("is_mapped", Boolean.class));

            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                view.setCreatedAt(createdAt.toLocalDateTime());
            }
            return view;
        }
    }
}
