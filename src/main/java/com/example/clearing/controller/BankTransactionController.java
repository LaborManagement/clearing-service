package com.example.clearing.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.clearing.domain.BankTransaction;
import com.example.clearing.model.AllocationBatchRequest;
import com.example.clearing.model.AllocationResponse;
import com.example.clearing.model.BankTransactionClaimRequest;
import com.example.clearing.model.BankTransactionClaimResult;
import com.example.clearing.model.BankTransactionDirectClaimRequest;
import com.example.clearing.model.BankTransactionView;
import com.example.clearing.service.AllocationService;
import com.example.clearing.service.BankTransactionClaimService;
import com.example.clearing.service.BankTransactionSearchService;
import com.shared.common.annotation.SecurePagination;
import com.shared.common.dto.SecurePaginationRequest;
import com.shared.common.dto.SecurePaginationResponse;
import com.shared.common.util.SecurePaginationUtil;
import com.shared.utilities.logger.LoggerFactoryProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reconciliation/bank-transactions")
@Tag(name = "Reconciliation Bank Transactions", description = "APIs to search bank transactions from reconciliation view")
@SecurityRequirement(name = "Bearer Authentication")
public class BankTransactionController {

    private static final Logger log = LoggerFactoryProvider.getLogger(BankTransactionController.class);
    private static final int MAX_PAGE_SIZE = 200;
    private static final List<String> SECURE_SORT_FIELDS = List.of("receiptDate", "createdAt", "amount", "id");

    private final BankTransactionSearchService searchService;
    private final BankTransactionClaimService claimService;
    private final AllocationService allocationService;

    public BankTransactionController(
            BankTransactionSearchService searchService,
            BankTransactionClaimService claimService,
            AllocationService allocationService) {
        this.searchService = searchService;
        this.claimService = claimService;
        this.allocationService = allocationService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions in reconciliation.vw_all_bank_transactions", description = "Filters by txn_date, amount, dr_cr_flag, bank_account_id, bank_account_nmbr, txn_ref; returns matching transactions without pagination metadata")
    public ResponseEntity<?> searchTransactions(
            @RequestParam(required = false) String txnDate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String drCrFlag,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(name = "bankAccountNmbr", required = false) String bankAccountNmbr,
            @RequestParam(name = "bankAccountNumber", required = false) String bankAccountNumberAlias,
            @RequestParam(required = false) String txnRef,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String bankAccountNumber = resolveAccountNumber(bankAccountNmbr, bankAccountNumberAlias);
            int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
            java.util.List<BankTransactionView> result = searchService.search(
                    parseTxnDate(txnDate),
                    amount,
                    drCrFlag,
                    bankAccountId,
                    bankAccountNumber,
                    txnRef,
                    safeSize);
            // Business case expects a single transaction; return only the list (no
            // pagination metadata)
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to search bank transactions", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to search bank transactions right now"));
        }
    }

    @PostMapping("/secure")
    @Operation(summary = "Secure paginated search of clearing.bank_transactions", description = "Mandatory date range with opaque page tokens; filters by amount, dr_cr_flag, bank_account_id, bank_account_nmbr, txn_ref")
    @SecurePagination
    public ResponseEntity<?> searchTransactionsSecure(
            @Valid @RequestBody SecurePaginationRequest request,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String drCrFlag,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(name = "bankAccountNmbr", required = false) String bankAccountNmbr,
            @RequestParam(name = "bankAccountNumber", required = false) String bankAccountNumberAlias,
            @RequestParam(required = false) String txnRef) {
        try {
            SecurePaginationUtil.applyPageToken(request);
            SecurePaginationUtil.ValidationResult validation = SecurePaginationUtil.validatePaginationRequest(request);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(SecurePaginationUtil.createErrorResponse(validation));
            }

            String bankAccountNumber = resolveAccountNumber(bankAccountNmbr, bankAccountNumberAlias);
            Sort sort = SecurePaginationUtil.createSecureSort(request, SECURE_SORT_FIELDS);
            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize(),
                    sort);

            Page<BankTransactionView> result = searchService.searchSecure(
                    validation.getStartDateTime().toLocalDate(),
                    validation.getEndDateTime().toLocalDate(),
                    amount,
                    drCrFlag,
                    bankAccountId,
                    bankAccountNumber,
                    txnRef,
                    request.getStatus(),
                    pageable);
            SecurePaginationResponse<BankTransactionView> response = SecurePaginationUtil.createSecureResponse(result,
                    request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to search bank transactions (secure paginated)", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to search bank transactions right now"));
        }
    }

    @GetMapping
    @Operation(summary = "List clearing bank transactions", description = "Filters by bankTxnId, txnRef (contains), isSettled for the caller's tenant")
    public ResponseEntity<?> listClearingBankTransactions(
            @RequestParam(name = "bankTxnId", required = false) Integer bankTxnId,
            @RequestParam(name = "txnRef", required = false) String txnRef,
            @RequestParam(name = "isSettled", required = false) Boolean isSettled,
            @RequestParam(defaultValue = "20") int size) {
        try {
            int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
            java.util.List<BankTransaction> txns = searchService.findClearingTransactions(
                    bankTxnId, txnRef, isSettled, safeSize);
            return ResponseEntity.ok(txns);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to list clearing bank transactions", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to fetch clearing bank transactions right now"));
        }
    }

    @PostMapping("/claim")
    @Operation(summary = "Claim a reconciliation transaction and import into clearing.bank_transaction")
    public ResponseEntity<?> claimTransaction(@Valid @RequestBody BankTransactionClaimRequest request) {
        String claimedBy = resolveClaimedBy(request.getClaimedBy());
        try {
            BankTransactionClaimResult result = claimService.claimFromRecon(
                    request.getType(), request.getSourceTxnId(), claimedBy);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to claim bank transaction {}", request.getSourceTxnId(), ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to claim bank transaction right now"));
        }
    }

    @PostMapping("/claim-direct")
    @Operation(summary = "Import a reconciliation transaction payload into clearing.bank_transaction without DB reach-through")
    public ResponseEntity<?> claimTransactionDirect(
            @Valid @RequestBody BankTransactionDirectClaimRequest request,
            @RequestParam(name = "claimedBy", required = false) String claimedBy) {
        String resolvedClaimedBy = resolveClaimedBy(claimedBy);
        try {
            BankTransactionClaimResult result = claimService.claimFromPayload(request, resolvedClaimedBy);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to import bank transaction {}", request.getSourceTxnId(), ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to import bank transaction right now"));
        }
    }

    @PostMapping("/allocations")
    @Operation(summary = "Create an allocation against a bank transaction and update remaining amount")
    public ResponseEntity<?> createAllocation(@Valid @RequestBody AllocationBatchRequest request) {
        try {
            List<AllocationResponse> responses = allocationService.createAllocations(request.getAllocations());
            if (request.isSingleAllocationPayload()) {
                return ResponseEntity.ok(responses.get(0));
            }
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            Integer logTxnId = request.getAllocations().isEmpty() ? null
                    : request.getAllocations().get(0).getBankTxnId();
            log.error("Failed to create allocation for txn {}", logTxnId, ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to create allocation right now"));
        }
    }

    private String resolveAccountNumber(String bankAccountNmbr, String bankAccountNumberAlias) {
        if (bankAccountNumberAlias != null && !bankAccountNumberAlias.trim().isEmpty()) {
            return bankAccountNumberAlias.trim();
        }
        if (bankAccountNmbr != null && !bankAccountNmbr.trim().isEmpty()) {
            return bankAccountNmbr.trim();
        }
        return null;
    }

    private String resolveClaimedBy(String claimedBy) {
        if (claimedBy == null || claimedBy.isBlank()) {
            return "system";
        }
        return claimedBy.trim();
    }

    private LocalDate parseTxnDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();
        // Try ISO first (yyyy-MM-dd), then d-MMM-uuuu (e.g., 14-OCT-2025),
        // case-insensitive.
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter dMmmYyyy = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("d-MMM-uuuu")
                .toFormatter(Locale.ENGLISH);
        for (DateTimeFormatter fmt : new DateTimeFormatter[] { iso, dMmmYyyy }) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException("Invalid txnDate format. Use yyyy-MM-dd or d-MMM-yyyy (e.g., 14-OCT-2025).");
    }
}
