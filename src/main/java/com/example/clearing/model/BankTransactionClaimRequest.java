package com.example.clearing.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BankTransactionClaimRequest {

    @NotBlank
    private String type;

    @NotNull
    private Long sourceTxnId;

    private String claimedBy;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSourceTxnId() {
        return sourceTxnId;
    }

    public void setSourceTxnId(Long sourceTxnId) {
        this.sourceTxnId = sourceTxnId;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }
}
