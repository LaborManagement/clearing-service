package com.example.clearing.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankTransactionSearchCriteria {

    private LocalDate txnDate;
    private BigDecimal amount;
    private String drCrFlag;
    private Long bankAccountId;
    private String bankAccountNumber;
    private String txnRef;
    private Integer statusId;

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

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public boolean hasAnyFilter() {
        return txnDate != null
                || amount != null
                || (drCrFlag != null && !drCrFlag.trim().isEmpty())
                || bankAccountId != null
                || (bankAccountNumber != null && !bankAccountNumber.trim().isEmpty())
                || (txnRef != null && !txnRef.trim().isEmpty())
                || statusId != null;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }
}
