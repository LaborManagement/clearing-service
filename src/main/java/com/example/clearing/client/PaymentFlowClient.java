package com.example.clearing.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.shared.utilities.logger.LoggerFactoryProvider;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class PaymentFlowClient {

    private static final Logger log = LoggerFactoryProvider.getLogger(PaymentFlowClient.class);

    private final RestTemplate restTemplate;

    @Value("${payment-flow.service.url}")
    private String paymentFlowServiceUrl;

    public PaymentFlowClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void updatePaymentStatus(Long paymentId, String status) {
        updateEmployerReceiptStatus(paymentId, status);
    }

    public void updatePaymentStatusById(Long paymentId, Long statusId) {
        updateEmployerReceiptStatusById(paymentId, statusId);
    }

    public void updateEmployerReceiptStatus(Long receiptId, String status) {
        String url = paymentFlowServiceUrl + "/api/internal/payments/employer-receipts/" + receiptId + "/status";
        log.info("Calling payment-flow service to update employer receipt status for receiptId: {}", receiptId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Forward the JWT token from the current request
        String jwtToken = extractJwtFromRequest();
        if (jwtToken != null) {
            headers.set("Authorization", "Bearer " + jwtToken);
            log.debug("Forwarding JWT token to payment-flow-service");
        } else {
            log.warn("No JWT token found in current request context");
        }

        Map<String, String> body = new HashMap<>();
        body.put("status", status);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully updated employer receipt status for receiptId: {}", receiptId);
            } else {
                log.error("Failed to update employer receipt status for receiptId: {}. Status code: {}", receiptId,
                        response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error calling payment-flow service for receiptId: {}. Status: {}, Body: {}",
                    receiptId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while calling payment-flow service for receiptId: {}", receiptId,
                    e);
            throw e;
        }
    }

    public void updateEmployerReceiptStatusById(Long receiptId, Long statusId) {
        String url = paymentFlowServiceUrl + "/api/internal/payments/employer-receipts/" + receiptId + "/status";
        log.info("Calling payment-flow service to update employer receipt status for receiptId: {} with statusId: {}",
                receiptId, statusId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Forward the JWT token from the current request
        String jwtToken = extractJwtFromRequest();
        if (jwtToken != null) {
            headers.set("Authorization", "Bearer " + jwtToken);
            log.debug("Forwarding JWT token to payment-flow-service");
        } else {
            log.warn("No JWT token found in current request context");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("statusId", statusId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully updated employer receipt status for receiptId: {} to statusId: {}",
                        receiptId, statusId);
            } else {
                log.error("Failed to update employer receipt status for receiptId: {}. Status code: {}", receiptId,
                        response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error calling payment-flow service for receiptId: {}. Status: {}, Body: {}",
                    receiptId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while calling payment-flow service for receiptId: {}", receiptId,
                    e);
            throw e;
        }
    }

    private String extractJwtFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract JWT from request context", e);
        }
        return null;
    }
}
