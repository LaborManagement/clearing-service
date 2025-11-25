package com.example.clearing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "voucher_line", schema = "clearing")
public class VoucherLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_line_id")
    private Integer voucherLineId;

    @Column(name = "voucher_id", nullable = false)
    private Integer voucherId;

    @Column(name = "dr_cr_flag", nullable = false, length = 2)
    private String drCrFlag;

    @Column(name = "gl_source_type", nullable = false)
    private String glSourceType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "bank_txn_id")
    private Long bankTxnId;

    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "dimension_source")
    private String dimensionSource;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Integer getVoucherLineId() {
        return voucherLineId;
    }

    public void setVoucherLineId(Integer voucherLineId) {
        this.voucherLineId = voucherLineId;
    }

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public String getDrCrFlag() {
        return drCrFlag;
    }

    public void setDrCrFlag(String drCrFlag) {
        this.drCrFlag = drCrFlag;
    }

    public String getGlSourceType() {
        return glSourceType;
    }

    public void setGlSourceType(String glSourceType) {
        this.glSourceType = glSourceType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Long bankTxnId) {
        this.bankTxnId = bankTxnId;
    }

    public Long getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Long allocationId) {
        this.allocationId = allocationId;
    }

    public String getDimensionSource() {
        return dimensionSource;
    }

    public void setDimensionSource(String dimensionSource) {
        this.dimensionSource = dimensionSource;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
