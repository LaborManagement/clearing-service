package com.example.clearing.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.clearing.dto.AllocationDetailsView;
import com.example.clearing.service.AllocationDetailsService;
import com.shared.common.annotation.SecurePagination;
import com.shared.common.dto.SecurePaginationRequest;
import com.shared.common.dto.SecurePaginationResponse;
import com.shared.common.util.SecurePaginationUtil;
import com.shared.utilities.logger.LoggerFactoryProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST API for querying allocation details combining voucher, payment
 * allocation,
 * bank transaction and request settlement data
 */
@RestController
@RequestMapping("/api/clearing/allocations")
@Tag(name = "Allocation Details", description = "APIs to query allocation details with voucher, payment allocation, bank transaction and settlement information")
@SecurityRequirement(name = "Bearer Authentication")
public class AllocationDetailsController {

    private static final Logger log = LoggerFactoryProvider.getLogger(AllocationDetailsController.class);

    // Allowed sort fields for secure pagination
    private static final List<String> SECURE_SORT_FIELDS = List.of(
            "receiptDate", "voucherDate", "txnDate", "receiptAmount", "txnAmount", "allocatedAmountFromTxn",
            "createdAt", "amount", "id");

    private final AllocationDetailsService allocationDetailsService;

    public AllocationDetailsController(AllocationDetailsService allocationDetailsService) {
        this.allocationDetailsService = allocationDetailsService;
    }

    @PostMapping("/details/secure")
    @Operation(summary = "Secure paginated search of allocation details", description = "Search allocation details with mandatory date range (filters on receipt_date). "
            +
            "Returns combined data from voucher_header, payment_allocation, bank_transaction and request_settlement. " +
            "Supports filters: employerReceiptNumber, voucherNumber, voucherDate range, txnDate range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Allocation details retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @SecurePagination
    public ResponseEntity<?> searchAllocationDetailsSecure(
            @Valid @RequestBody SecurePaginationRequest request,

            @Parameter(description = "Employer receipt number (exact match)") @RequestParam(required = false) String employerReceiptNumber,

            @Parameter(description = "Voucher number (exact match)") @RequestParam(required = false) String voucherNumber,

            @Parameter(description = "Voucher date start (YYYY-MM-DD)") @RequestParam(required = false) String voucherDateStart,

            @Parameter(description = "Voucher date end (YYYY-MM-DD)") @RequestParam(required = false) String voucherDateEnd,

            @Parameter(description = "Transaction date start (YYYY-MM-DD)") @RequestParam(required = false) String txnDateStart,

            @Parameter(description = "Transaction date end (YYYY-MM-DD)") @RequestParam(required = false) String txnDateEnd) {

        try {
            // Apply page token if present
            SecurePaginationUtil.applyPageToken(request);

            // Validate pagination request
            SecurePaginationUtil.ValidationResult validation = SecurePaginationUtil.validatePaginationRequest(request);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                        .body(SecurePaginationUtil.createErrorResponse(validation));
            }

            // Parse optional date filters
            LocalDate voucherDateStartParsed = parseDate(voucherDateStart);
            LocalDate voucherDateEndParsed = parseDate(voucherDateEnd);
            LocalDate txnDateStartParsed = parseDate(txnDateStart);
            LocalDate txnDateEndParsed = parseDate(txnDateEnd);

            // Create sort and pageable
            Sort sort = SecurePaginationUtil.createSecureSort(request, SECURE_SORT_FIELDS);
            if (sort == null) {
                sort = Sort.by(Sort.Direction.DESC, "receiptDate");
            }
            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize(),
                    sort);

            // Search allocation details
            Page<AllocationDetailsView> result = allocationDetailsService.searchAllocationDetails(
                    validation.getStartDateTime().toLocalDate(),
                    validation.getEndDateTime().toLocalDate(),
                    employerReceiptNumber,
                    voucherNumber,
                    voucherDateStartParsed,
                    voucherDateEndParsed,
                    txnDateStartParsed,
                    txnDateEndParsed,
                    pageable);

            // Create secure pagination response
            SecurePaginationResponse<AllocationDetailsView> response = SecurePaginationUtil.createSecureResponse(result,
                    request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid request for allocation details search: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to search allocation details (secure paginated)", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to search allocation details right now"));
        }
    }

    /**
     * Parse date string in YYYY-MM-DD format
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for '" + dateStr + "'. Use YYYY-MM-DD.");
        }
    }
}
