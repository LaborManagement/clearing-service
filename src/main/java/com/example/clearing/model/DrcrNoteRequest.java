package com.example.clearing.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class DrcrNoteRequest {

    @NotNull
    private Long requestId;

    @NotBlank
    @Size(max = 10)
    private String voucherType;

    @Size(max = 50)
    private String narationType;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    private Integer toliId;

    @Size(max = 100)
    private String createdBy;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getNarationType() {
        return narationType;
    }

    public void setNarationType(String narationType) {
        this.narationType = narationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getToliId() {
        return toliId;
    }

    public void setToliId(Integer toliId) {
        this.toliId = toliId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
