package com.example.clearing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bank_transaction", schema = "clearing")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_txn_id")
    private Integer bankTxnId;

    @Column(name = "bank_account_id", nullable = false)
    private Integer bankAccountId;

    @Column(name = "txn_ref")
    private String txnRef;

    @Column(name = "txn_date")
    private LocalDate txnDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "dr_cr_flag", length = 2, nullable = false)
    private String drCrFlag;

    private String description;

    @Column(name = "allocated_amount")
    private BigDecimal allocatedAmount;

    @Column(name = "remaining_amount")
    private BigDecimal remainingAmount;

    @Deprecated
    private String status;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "board_id")
    private Integer boardId;

    @Column(name = "employer_id")
    private Integer employerId;

    @Column(name = "toli_id")
    private Integer toliId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "source_txn_id")
    private String sourceTxnId;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "claimed_by")
    private String claimedBy;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "is_settled")
    private Boolean isSettled;

    @Transient
    private String statusCode;

    @Version
    private Integer version;

    public Integer getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Integer bankTxnId) {
        this.bankTxnId = bankTxnId;
    }

    public Integer getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Integer bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDrCrFlag() {
        return drCrFlag;
    }

    public void setDrCrFlag(String drCrFlag) {
        this.drCrFlag = drCrFlag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public Integer getBoardId() {
        return boardId;
    }

    public void setBoardId(Integer boardId) {
        this.boardId = boardId;
    }

    public Integer getEmployerId() {
        return employerId;
    }

    public void setEmployerId(Integer employerId) {
        this.employerId = employerId;
    }

    public Integer getToliId() {
        return toliId;
    }

    public void setToliId(Integer toliId) {
        this.toliId = toliId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceTxnId() {
        return sourceTxnId;
    }

    public void setSourceTxnId(String sourceTxnId) {
        this.sourceTxnId = sourceTxnId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public OffsetDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(OffsetDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Boolean getIsSettled() {
        return isSettled;
    }

    public void setIsSettled(Boolean isSettled) {
        this.isSettled = isSettled;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
