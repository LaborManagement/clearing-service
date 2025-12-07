package com.example.clearing.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BankTransactionClaimRequest {

    @NotBlank
    private String type;

    @NotNull
    private Long sourceTxnId;

    private String claimedBy;

    @JsonProperty("internal_ref")
    @JsonAlias("internalRef")
    private String internalRef;

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

    public String getInternalRef() {
        return internalRef;
    }

    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }
}
