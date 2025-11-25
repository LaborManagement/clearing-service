package com.example.clearing.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AllocationBreakdown {

    private Long allocationId;

    @NotNull
    private Long bankTxnId;

    @NotNull
    private BigDecimal amount;

    public Long getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Long allocationId) {
        this.allocationId = allocationId;
    }

    public Long getBankTxnId() {
        return bankTxnId;
    }

    public void setBankTxnId(Long bankTxnId) {
        this.bankTxnId = bankTxnId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
