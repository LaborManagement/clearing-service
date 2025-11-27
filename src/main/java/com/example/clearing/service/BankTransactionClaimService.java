package com.example.clearing.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.clearing.model.BankTransactionClaimResult;
import com.shared.common.dao.TenantAccessDao;

import jakarta.transaction.Transactional;

@Service
public class BankTransactionClaimService {

    private static final String SOURCE_SYSTEM = "RECON";
    private static final String STATUS_TYPE_BANK_TXN = "bank_transaction";
    private static final int STATUS_ID_CLAIMED = 1;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StatusService statusService;
    private final TenantAccessDao tenantAccessDao;

    public BankTransactionClaimService(
            NamedParameterJdbcTemplate jdbcTemplate,
            StatusService statusService,
            TenantAccessDao tenantAccessDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.statusService = statusService;
        this.tenantAccessDao = tenantAccessDao;
    }

    @Transactional
    public BankTransactionClaimResult claimFromRecon(String type, Long sourceTxnId, String claimedBy) {
        String normalizedType = normalizeType(type);
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        SourceTxnRow sourceTxn = fetchAndLockSource(normalizedType, sourceTxnId);
        if (Boolean.TRUE.equals(sourceTxn.isMapped)) {
            throw new IllegalStateException("Transaction already mapped/claimed: " + sourceTxnId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        markSourceMapped(normalizedType, sourceTxnId);
        Integer bankTxnId = upsertClearingBankTxn(normalizedType, sourceTxn, claimedBy, now, tenantAccess);

        BankTransactionClaimResult result = new BankTransactionClaimResult();
        result.setBankTxnId(bankTxnId);
        result.setTxnType(normalizedType);
        result.setSourceSystem(SOURCE_SYSTEM);
        result.setSourceTxnId(sourceTxnId);
        result.setBankAccountId(sourceTxn.bankAccountId);
        result.setTxnRef(sourceTxn.txnRef);
        result.setTxnDate(sourceTxn.txnDate);
        result.setAmount(sourceTxn.amount);
        result.setDrCrFlag(sourceTxn.drCrFlag);
        result.setDescription(sourceTxn.description);
        result.setClaimedBy(claimedBy);
        result.setClaimedAt(now);
        return result;
    }

    private TenantAccessDao.TenantAccess requireTenantAccess() {
        TenantAccessDao.TenantAccess tenantAccess = tenantAccessDao.getFirstAccessibleTenant();
        if (tenantAccess == null || tenantAccess.boardId == null || tenantAccess.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) assigned for clearing claim");
        }
        return tenantAccess;
    }

    private SourceTxnRow fetchAndLockSource(String type, Long sourceTxnId) {
        if (sourceTxnId == null) {
            throw new IllegalArgumentException("sourceTxnId is required");
        }
        if ("MT940".equals(type) || "CAMT53".equals(type)) {
            return fetchStatementTxn(sourceTxnId);
        }
        if ("VAN".equals(type)) {
            return fetchVanTxn(sourceTxnId);
        }
        throw new IllegalArgumentException("Unsupported transaction type: " + type);
    }

    private SourceTxnRow fetchStatementTxn(Long id) {
        String sql = """
                SELECT id AS source_txn_id,
                       statement_file_id::bigint AS bank_account_id,
                       bank_reference AS txn_ref,
                       value_date AS txn_date,
                       amount,
                       dc AS dr_cr_flag,
                       narrative AS description,
                       COALESCE(is_mapped, FALSE) AS is_mapped
                  FROM reconciliation.statement_transaction
                 WHERE id = :id
                 FOR UPDATE
                """;
        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    Map.of("id", id),
                    (rs, rowNum) -> {
                        SourceTxnRow row = new SourceTxnRow();
                        row.sourceTxnId = rs.getLong("source_txn_id");
                        row.bankAccountId = rs.getLong("bank_account_id");
                        row.txnRef = rs.getString("txn_ref");
                        Date txnDate = rs.getDate("txn_date");
                        row.txnDate = txnDate != null ? txnDate.toLocalDate() : null;
                        row.amount = rs.getBigDecimal("amount");
                        row.drCrFlag = rs.getString("dr_cr_flag");
                        row.description = rs.getString("description");
                        row.isMapped = rs.getBoolean("is_mapped");
                        return row;
                    });
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("statement_transaction not found: " + id);
        }
    }

    private SourceTxnRow fetchVanTxn(Long id) {
        String sql = """
                SELECT id AS source_txn_id,
                       import_run_id::bigint AS bank_account_id,
                       transaction_reference_number AS txn_ref,
                       COALESCE(value_date, transaction_date) AS txn_date,
                       amount,
                       'CR' AS dr_cr_flag,
                       payment_description_narration AS description,
                       COALESCE(is_mapped, FALSE) AS is_mapped
                  FROM reconciliation.van_transaction
                 WHERE id = :id
                 FOR UPDATE
                """;
        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    Map.of("id", id),
                    (rs, rowNum) -> {
                        SourceTxnRow row = new SourceTxnRow();
                        row.sourceTxnId = rs.getLong("source_txn_id");
                        row.bankAccountId = rs.getLong("bank_account_id");
                        row.txnRef = rs.getString("txn_ref");
                        Date txnDate = rs.getDate("txn_date");
                        row.txnDate = txnDate != null ? txnDate.toLocalDate() : null;
                        row.amount = rs.getBigDecimal("amount");
                        row.drCrFlag = rs.getString("dr_cr_flag");
                        row.description = rs.getString("description");
                        row.isMapped = rs.getBoolean("is_mapped");
                        return row;
                    });
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("van_transaction not found: " + id);
        }
    }

    private void markSourceMapped(String type, Long id) {
        String table = "MT940".equals(type) || "CAMT53".equals(type)
                ? "reconciliation.statement_transaction"
                : "reconciliation.van_transaction";
        String sql = "UPDATE %s SET is_mapped = TRUE WHERE id = :id AND COALESCE(is_mapped, FALSE) = FALSE"
                .formatted(table);
        int updated = jdbcTemplate.update(
                sql,
                Map.of("id", id));
        if (updated == 0) {
            throw new IllegalStateException("Transaction already mapped/claimed: " + id);
        }
    }

    private Integer upsertClearingBankTxn(
            String type,
            SourceTxnRow row,
            String claimedBy,
            OffsetDateTime claimedAt,
            TenantAccessDao.TenantAccess tenantAccess) {
        String sql = """
                INSERT INTO clearing.bank_transaction (
                    bank_account_id,
                    txn_ref,
                    txn_date,
                    amount,
                    dr_cr_flag,
                    description,
                    allocated_amount,
                    remaining_amount,
                    status,
                    status_id,
                    board_id,
                    employer_id,
                    toli_id,
                    created_at,
                    updated_at,
                    txn_type,
                    source_system,
                    source_txn_id,
                    source_ref,
                    claimed_by,
                    claimed_at,
                    is_settled
                )
                VALUES (
                    :bankAccountId,
                    :txnRef,
                    :txnDate,
                    :amount,
                    :drCrFlag,
                    :description,
                    0,
                    :amount,
                    1,
                    :statusId,
                    :boardId,
                    :employerId,
                    :toliId,
                    :claimedAt,
                    :claimedAt,
                    :txnType,
                    :sourceSystem,
                    :sourceTxnId,
                    :sourceRef,
                    :claimedBy,
                    :claimedAt,
                    FALSE
                )
                ON CONFLICT (source_system, source_txn_id)
                DO UPDATE SET
                    claimed_by = EXCLUDED.claimed_by,
                    claimed_at = EXCLUDED.claimed_at,
                    description = COALESCE(EXCLUDED.description, clearing.bank_transaction.description),
                    txn_ref = COALESCE(EXCLUDED.txn_ref, clearing.bank_transaction.txn_ref),
                    txn_date = COALESCE(EXCLUDED.txn_date, clearing.bank_transaction.txn_date),
                    amount = COALESCE(EXCLUDED.amount, clearing.bank_transaction.amount),
                    dr_cr_flag = COALESCE(EXCLUDED.dr_cr_flag, clearing.bank_transaction.dr_cr_flag),
                    status_id = COALESCE(EXCLUDED.status_id, clearing.bank_transaction.status_id),
                    board_id = COALESCE(EXCLUDED.board_id, clearing.bank_transaction.board_id),
                    employer_id = COALESCE(EXCLUDED.employer_id, clearing.bank_transaction.employer_id),
                    toli_id = COALESCE(EXCLUDED.toli_id, clearing.bank_transaction.toli_id),
                    updated_at = EXCLUDED.updated_at
                RETURNING bank_txn_id;
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bankAccountId", row.bankAccountId)
                .addValue("txnRef", row.txnRef)
                .addValue("txnDate", row.txnDate)
                .addValue("amount", row.amount)
                .addValue("drCrFlag", row.drCrFlag)
                .addValue("description", row.description)
                .addValue("claimedAt", claimedAt)
                .addValue("txnType", type)
                .addValue("sourceSystem", SOURCE_SYSTEM)
                .addValue("sourceTxnId", row.sourceTxnId)
                .addValue("sourceRef", row.txnRef)
                .addValue("claimedBy", claimedBy)
                .addValue("statusId", STATUS_ID_CLAIMED)
                .addValue("boardId", tenantAccess.boardId)
                .addValue("employerId", tenantAccess.employerId)
                .addValue("toliId", tenantAccess.toliId);

        List<Integer> ids = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getInt("bank_txn_id"));
        if (ids.isEmpty()) {
            throw new IllegalStateException("Failed to upsert clearing.bank_transaction for source " + row.sourceTxnId);
        }
        return ids.get(0);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type is required");
        }
        return type.trim().toUpperCase();
    }

    private static class SourceTxnRow {
        Long sourceTxnId;
        Long bankAccountId;
        String txnRef;
        LocalDate txnDate;
        BigDecimal amount;
        String drCrFlag;
        String description;
        Boolean isMapped;
    }
}
