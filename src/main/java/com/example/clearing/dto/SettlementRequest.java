package com.example.clearing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class SettlementRequest {

    @NotNull
    private Long requestId;

    private Long boardId;
    private Long employerId;

    @NotNull
    private BigDecimal totalAmount;

    private String idempotencyKey;

    @Valid
    @NotEmpty
    private List<AllocationBreakdown> allocations;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public Long getEmployerId() {
        return employerId;
    }

    public void setEmployerId(Long employerId) {
        this.employerId = employerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<AllocationBreakdown> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<AllocationBreakdown> allocations) {
        this.allocations = allocations;
    }
}
