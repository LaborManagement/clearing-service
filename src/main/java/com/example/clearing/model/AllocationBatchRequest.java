package com.example.clearing.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Describes a batch of allocations so the controller can execute multiple
 * request-bank pairings in one transaction while remaining backwards compatible
 * with single-allocation payloads.
 */
@Schema(description = "Batch of bank transaction allocations.")
@JsonDeserialize(using = AllocationBatchRequestDeserializer.class)
public class AllocationBatchRequest {

    @Schema(description = "Allocations to process", required = true)
    @NotEmpty(message = "At least one allocation must be provided")
    @Valid
    private final List<AllocationRequest> allocations;

    private final boolean singleAllocationPayload;

    AllocationBatchRequest(List<AllocationRequest> allocations, boolean singleAllocationPayload) {
        this.allocations = List.copyOf(allocations);
        this.singleAllocationPayload = singleAllocationPayload;
    }

    public List<AllocationRequest> getAllocations() {
        return allocations;
    }

    public boolean isSingleAllocationPayload() {
        return singleAllocationPayload;
    }
}
