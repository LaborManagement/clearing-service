package com.example.clearing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "payment_allocation", schema = "clearing")
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Integer allocationId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "bank_txn_id", nullable = false)
    private Integer bankTxnId;

    @Column(name = "board_id", nullable = false)
    private Integer boardId;

    @Column(name = "employer_id", nullable = false)
    private Integer employerId;

    @Column(name = "toli_id")
    private Integer toliId;

    @Column(name = "allocated_amount", nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "allocated_by")
    private String allocatedBy;

    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "source_file_id")
    private Integer sourceFileId;

    @Column(name = "status_id")
    private Integer statusId;

    @Transient
    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    private Integer version;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "employer_receipt_number", length = 40)
    private String employerReceiptNumber;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    public Integer getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Integer allocationId) {
        this.allocationId = allocationId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Integer getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Integer bankTxnId) {
        this.bankTxnId = bankTxnId;
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

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public LocalDate getAllocationDate() {
        return allocationDate;
    }

    public void setAllocationDate(LocalDate allocationDate) {
        this.allocationDate = allocationDate;
    }

    public String getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(String allocatedBy) {
        this.allocatedBy = allocatedBy;
    }

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public Integer getSourceFileId() {
        return sourceFileId;
    }

    public void setSourceFileId(Integer sourceFileId) {
        this.sourceFileId = sourceFileId;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getEmployerReceiptNumber() {
        return employerReceiptNumber;
    }

    public void setEmployerReceiptNumber(String employerReceiptNumber) {
        this.employerReceiptNumber = employerReceiptNumber;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }
}
