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

    public StatusService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void warmCache() {
        // Preload all statuses; non-fatal if fails.
        try {
            jdbcTemplate.query(
                    "SELECT status_type, status_code, id FROM clearing.status_master WHERE is_active = TRUE",
                    rs -> {
                        String key = key(rs.getString("status_type"), rs.getString("status_code"));
                        cache.put(key, rs.getInt("id"));
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
        Integer id = jdbcTemplate.query(
                "SELECT id FROM clearing.status_master WHERE status_type = :type AND status_code = :code AND is_active = TRUE",
                Map.of("type", statusType, "code", statusCode),
                rs -> rs.next() ? rs.getInt("id") : null);
        if (id == null) {
            throw new IllegalStateException("Status not found for type=" + statusType + " code=" + statusCode);
        }
        cache.put(key, id);
        return id;
    }

    private String key(String type, String code) {
        return type + "::" + code;
    }
}
