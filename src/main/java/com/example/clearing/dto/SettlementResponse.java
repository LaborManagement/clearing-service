package com.example.clearing.dto;

import java.math.BigDecimal;

public class SettlementResponse {

    private Integer eventId;
    private Integer voucherId;
    private String voucherStatus;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String message;

    public SettlementResponse() {
    }

    public SettlementResponse(Integer eventId, Integer voucherId, String voucherStatus, BigDecimal totalDebit, BigDecimal totalCredit, String message) {
        this.eventId = eventId;
        this.voucherId = voucherId;
        this.voucherStatus = voucherStatus;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.message = message;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherStatus() {
        return voucherStatus;
    }

    public void setVoucherStatus(String voucherStatus) {
        this.voucherStatus = voucherStatus;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
