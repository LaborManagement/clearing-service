package com.example.clearing.controller;

import com.example.clearing.domain.DrcrNote;
import com.example.clearing.model.DrcrNoteRequest;
import com.example.clearing.service.DrcrNoteService;
import com.shared.utilities.logger.LoggerFactoryProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clearing-service/api/clearing/drcr-notes")
@SecurityRequirement(name = "Bearer Authentication")
public class DrcrNoteController {

    private static final Logger log = LoggerFactoryProvider.getLogger(DrcrNoteController.class);

    private final DrcrNoteService drcrNoteService;

    public DrcrNoteController(DrcrNoteService drcrNoteService) {
        this.drcrNoteService = drcrNoteService;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(name = "requestId", required = false) Long requestId,
            @RequestParam(name = "voucherType", required = false) String voucherType,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        try {
            List<DrcrNote> result = drcrNoteService.list(requestId, voucherType, limit);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to list drcr notes", ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to list drcr notes right now"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        try {
            DrcrNote note = drcrNoteService.get(id);
            return ResponseEntity.ok(note);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to fetch drcr note {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to fetch drcr note right now"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DrcrNoteRequest request) {
        try {
            DrcrNote created = drcrNoteService.create(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to create drcr note for request {}", request.getRequestId(), ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to create drcr note right now"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @Valid @RequestBody DrcrNoteRequest request) {
        try {
            DrcrNote updated = drcrNoteService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to update drcr note {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to update drcr note right now"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        try {
            drcrNoteService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to delete drcr note {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to delete drcr note right now"));
        }
    }
}
