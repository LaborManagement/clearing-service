package com.example.clearing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounting_rule_line", schema = "clearing")
public class AccountingRuleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_line_id")
    private Integer ruleLineId;

    @Column(name = "rule_header_id", nullable = false)
    private Integer ruleHeaderId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "dr_cr_flag", nullable = false, length = 2)
    private String drCrFlag;

    @Column(name = "gl_source_type", nullable = false)
    private String glSourceType;

    @Column(name = "amount_source", nullable = false)
    private String amountSource;

    @Column(name = "dimension_source")
    private String dimensionSource;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Integer getRuleLineId() {
        return ruleLineId;
    }

    public void setRuleLineId(Integer ruleLineId) {
        this.ruleLineId = ruleLineId;
    }

    public Integer getRuleHeaderId() {
        return ruleHeaderId;
    }

    public void setRuleHeaderId(Integer ruleHeaderId) {
        this.ruleHeaderId = ruleHeaderId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
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

    public String getAmountSource() {
        return amountSource;
    }

    public void setAmountSource(String amountSource) {
        this.amountSource = amountSource;
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
