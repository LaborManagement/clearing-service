package com.example.clearing.service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Lightweight status lookup service backed by clearing.status_master.
 */
@Service
public class StatusService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();
    private final Map<String, String> codeCache = new ConcurrentHashMap<>();

    public StatusService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void warmCache() {
        // Preload all statuses; non-fatal if fails.
        try {
            jdbcTemplate.query(
                    "SELECT status_type, status_code, seq_no FROM clearing.status_master WHERE is_active = TRUE",
                    rs -> {
                        String type = rs.getString("status_type");
                        String code = rs.getString("status_code");
                        Integer seq = rs.getInt("seq_no");
                        cache.put(key(type, code), seq);
                        codeCache.put(codeKey(type, seq), code);
                    });
        } catch (Exception ignored) {
            // cache will lazily load on first miss
        }
    }

    public Integer requireStatusId(String statusType, String statusCode) {
        String key = key(statusType, statusCode);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Integer seqNo = jdbcTemplate.query(
                "SELECT seq_no FROM clearing.status_master WHERE status_type = :type AND status_code = :code AND is_active = TRUE",
                Map.of("type", statusType, "code", statusCode),
                rs -> rs.next() ? rs.getInt("seq_no") : null);
        if (seqNo == null) {
            throw new IllegalStateException("Status not found for type=" + statusType + " code=" + statusCode);
        }
        cache.put(key, seqNo);
        return seqNo;
    }

    public String resolveStatusCode(String statusType, Integer statusId) {
        if (statusId == null) {
            return null;
        }
        String cacheKey = codeKey(statusType, statusId);
        String cached = codeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String code = jdbcTemplate.query(
                "SELECT status_code FROM clearing.status_master WHERE status_type = :type AND seq_no = :seq AND is_active = TRUE",
                Map.of("type", statusType, "seq", statusId),
                rs -> rs.next() ? rs.getString("status_code") : null);
        if (code != null) {
            codeCache.put(cacheKey, code);
        }
        return code;
    }

    private String key(String type, String code) {
        return type + "::" + code;
    }

    private String codeKey(String type, Integer seqNo) {
        return type + "::" + seqNo;
    }
}
