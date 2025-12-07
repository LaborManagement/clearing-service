package com.example.clearing.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class AllocationRequest {

    @NotNull
    private Long requestId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal requestedAmount;

    @NotNull
    private Integer bankTxnId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal allocatedAmount;

    private LocalDate allocationDate;
    private String allocatedBy;
    private String idempotencyKey;

    @Schema(description = "Employer receipt number from payment_flow.employer_payment_receipts", example = "EMP-20251206-160818-106", maxLength = 40)
    private String employerReceiptNumber;

    @Schema(description = "Receipt date from employer payment receipt", example = "2025-12-06")
    private LocalDate receiptDate;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Integer bankTxnId) {
        this.bankTxnId = bankTxnId;
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
