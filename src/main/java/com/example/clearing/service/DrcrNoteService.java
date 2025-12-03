package com.example.clearing.service;

import com.example.clearing.domain.DrcrNote;
import com.example.clearing.model.DrcrNoteRequest;
import com.example.clearing.repository.DrcrNoteRepository;
import com.shared.common.dao.TenantAccessDao;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DrcrNoteService {

    private static final int MAX_LIMIT = 200;

    private final DrcrNoteRepository drcrNoteRepository;
    private final TenantAccessDao tenantAccessDao;

    public DrcrNoteService(DrcrNoteRepository drcrNoteRepository, TenantAccessDao tenantAccessDao) {
        this.drcrNoteRepository = drcrNoteRepository;
        this.tenantAccessDao = tenantAccessDao;
    }

    @Transactional
    public DrcrNote create(DrcrNoteRequest request) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        DrcrNote note = new DrcrNote();
        OffsetDateTime now = OffsetDateTime.now();
        applyRequest(note, request, tenantAccess, null);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        note.setCreatedBy(resolveCreatedBy(request.getCreatedBy()));
        return drcrNoteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<DrcrNote> list(Long requestId, String voucherType, int limit) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        return drcrNoteRepository.search(
                tenantAccess.boardId,
                tenantAccess.employerId,
                requestId,
                trimToNull(voucherType),
                PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "updatedAt", "id")));
    }

    @Transactional(readOnly = true)
    public DrcrNote get(Long id) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        return drcrNoteRepository
                .findByIdAndBoardIdAndEmployerId(id, tenantAccess.boardId, tenantAccess.employerId)
                .orElseThrow(() -> new IllegalArgumentException("drcr_note not found: " + id));
    }

    @Transactional
    public DrcrNote update(Long id, DrcrNoteRequest request) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        DrcrNote existing = drcrNoteRepository
                .findByIdAndBoardIdAndEmployerId(id, tenantAccess.boardId, tenantAccess.employerId)
                .orElseThrow(() -> new IllegalArgumentException("drcr_note not found: " + id));
        applyRequest(existing, request, tenantAccess, existing.getToliId());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (request.getCreatedBy() != null) {
            existing.setCreatedBy(trimToNull(request.getCreatedBy()));
        }
        return drcrNoteRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        TenantAccessDao.TenantAccess tenantAccess = requireTenantAccess();
        DrcrNote existing = drcrNoteRepository
                .findByIdAndBoardIdAndEmployerId(id, tenantAccess.boardId, tenantAccess.employerId)
                .orElseThrow(() -> new IllegalArgumentException("drcr_note not found: " + id));
        drcrNoteRepository.delete(existing);
    }

    private void applyRequest(
            DrcrNote target, DrcrNoteRequest request, TenantAccessDao.TenantAccess tenantAccess, Integer currentToliId) {
        validateAmount(request.getAmount());
        target.setRequestId(request.getRequestId());
        target.setVoucherType(request.getVoucherType().trim());
        target.setNarationType(trimToNull(request.getNarationType()));
        target.setAmount(request.getAmount());
        target.setDescription(trimToNull(request.getDescription()));
        target.setBoardId(tenantAccess.boardId);
        target.setEmployerId(tenantAccess.employerId);
        target.setToliId(resolveToliId(request.getToliId(), tenantAccess.toliId, currentToliId));
    }

    private Integer resolveToliId(Integer requested, Integer tenantToli, Integer current) {
        if (tenantToli != null) {
            if (requested != null && !tenantToli.equals(requested)) {
                throw new IllegalStateException("Toli mismatch with tenant access");
            }
            return tenantToli;
        }
        if (requested != null) {
            return requested;
        }
        return current;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveCreatedBy(String raw) {
        String trimmed = trimToNull(raw);
        return trimmed != null ? trimmed : "system";
    }

    private TenantAccessDao.TenantAccess requireTenantAccess() {
        TenantAccessDao.TenantAccess ta = tenantAccessDao.getFirstAccessibleTenant();
        if (ta == null || ta.boardId == null || ta.employerId == null) {
            throw new IllegalStateException("User has no tenant access (board/employer) to manage drcr notes");
        }
        return ta;
    }
}
