package com.example.clearing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for allocation details view combining voucher, payment allocation,
 * bank transaction and request settlement data
 */
@Schema(description = "Allocation details combining voucher, payment allocation, bank transaction and settlement information")
public class AllocationDetailsView {

    @Schema(description = "Voucher number", example = "VCH-2025-001")
    private String voucherNumber;

    @Schema(description = "Voucher date", example = "2025-12-07")
    private LocalDate voucherDate;

    @Schema(description = "Employer receipt number from payment allocation", example = "EMP-20251206-160818-106")
    private String employerReceiptNumber;

    @Schema(description = "Worker receipt number linked to the employer receipt", example = "RCP-20251206-160818-106")
    private String workerReceiptNumber;

    @Schema(description = "Receipt date from payment allocation", example = "2025-12-06")
    private LocalDate receiptDate;

    @Schema(description = "Total receipt amount from request settlement")
    private BigDecimal receiptAmount;

    @Schema(description = "Bank transaction ID")
    private Long bankTxnId;

    @Schema(description = "Bank transaction internal reference", example = "INT-REF-001")
    private String internalRef;

    @Schema(description = "Bank transaction reference", example = "TRF1")
    private String txnRef;

    @Schema(description = "Bank transaction amount")
    private BigDecimal txnAmount;

    @Schema(description = "Allocated amount from this transaction")
    private BigDecimal allocatedAmountFromTxn;

    @Schema(description = "Remaining amount of the bank transaction")
    private BigDecimal remainingAmountOfTxn;

    @Schema(description = "Bank transaction date", example = "2025-12-06")
    private LocalDate txnDate;

    // Constructors
    public AllocationDetailsView() {
    }

    public AllocationDetailsView(String voucherNumber, LocalDate voucherDate,
            String employerReceiptNumber, String workerReceiptNumber, LocalDate receiptDate,
            BigDecimal receiptAmount, Long bankTxnId,
            String internalRef, String txnRef,
            BigDecimal txnAmount, BigDecimal allocatedAmountFromTxn,
            BigDecimal remainingAmountOfTxn, LocalDate txnDate) {
        this.voucherNumber = voucherNumber;
        this.voucherDate = voucherDate;
        this.employerReceiptNumber = employerReceiptNumber;
        this.workerReceiptNumber = workerReceiptNumber;
        this.receiptDate = receiptDate;
        this.receiptAmount = receiptAmount;
        this.bankTxnId = bankTxnId;
        this.internalRef = internalRef;
        this.txnRef = txnRef;
        this.txnAmount = txnAmount;
        this.allocatedAmountFromTxn = allocatedAmountFromTxn;
        this.remainingAmountOfTxn = remainingAmountOfTxn;
        this.txnDate = txnDate;
    }

    // Getters and Setters
    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getEmployerReceiptNumber() {
        return employerReceiptNumber;
    }

    public void setEmployerReceiptNumber(String employerReceiptNumber) {
        this.employerReceiptNumber = employerReceiptNumber;
    }

    public String getWorkerReceiptNumber() {
        return workerReceiptNumber;
    }

    public void setWorkerReceiptNumber(String workerReceiptNumber) {
        this.workerReceiptNumber = workerReceiptNumber;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getReceiptAmount() {
        return receiptAmount;
    }

    public void setReceiptAmount(BigDecimal receiptAmount) {
        this.receiptAmount = receiptAmount;
    }

    public Long getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Long bankTxnId) {
        this.bankTxnId = bankTxnId;
    }

    public String getInternalRef() {
        return internalRef;
    }

    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public BigDecimal getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(BigDecimal txnAmount) {
        this.txnAmount = txnAmount;
    }

    public BigDecimal getAllocatedAmountFromTxn() {
        return allocatedAmountFromTxn;
    }

    public void setAllocatedAmountFromTxn(BigDecimal allocatedAmountFromTxn) {
        this.allocatedAmountFromTxn = allocatedAmountFromTxn;
    }

    public BigDecimal getRemainingAmountOfTxn() {
        return remainingAmountOfTxn;
    }

    public void setRemainingAmountOfTxn(BigDecimal remainingAmountOfTxn) {
        this.remainingAmountOfTxn = remainingAmountOfTxn;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    @Override
    public String toString() {
        return "AllocationDetailsView{" +
                "voucherNumber='" + voucherNumber + '\'' +
                ", voucherDate=" + voucherDate +
                ", employerReceiptNumber='" + employerReceiptNumber + '\'' +
                ", workerReceiptNumber='" + workerReceiptNumber + '\'' +
                ", receiptDate=" + receiptDate +
                ", receiptAmount=" + receiptAmount +
                ", bankTxnId=" + bankTxnId +
                ", internalRef='" + internalRef + '\'' +
                ", txnRef='" + txnRef + '\'' +
                ", txnAmount=" + txnAmount +
                ", allocatedAmountFromTxn=" + allocatedAmountFromTxn +
                ", remainingAmountOfTxn=" + remainingAmountOfTxn +
                ", txnDate=" + txnDate +
                '}';
    }
}
