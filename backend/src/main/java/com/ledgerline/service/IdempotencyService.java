package com.ledgerline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.entity.IdempotencyRecord;
import com.ledgerline.enums.IdempotencyStatus;
import com.ledgerline.exception.IdempotencyConflictException;
import com.ledgerline.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    public String computePayloadHash(String endpoint, Object requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String serialized = endpoint + ":" + (requestBody != null ? objectMapper.writeValueAsString(requestBody) : "");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to calculate request hash for idempotency", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> checkOrCreate(String idempotencyKey, Long userId, String requestHash) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' has already been used with different request parameters.");
            }
            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new IdempotencyConflictException(
                        "A request with idempotency key '" + idempotencyKey + "' is currently in progress. Please retry shortly.");
            }
            return Optional.of(record);
        }

        try {
            IdempotencyRecord newRecord = new IdempotencyRecord(idempotencyKey, userId, requestHash);
            newRecord = idempotencyRecordRepository.saveAndFlush(newRecord);
            return Optional.empty(); // Means newly created, proceed with execution
        } catch (DataIntegrityViolationException e) {
            // Concurrent insert race condition caught by DB unique constraint
            IdempotencyRecord concurrentRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IdempotencyConflictException("Concurrent idempotency lock conflict."));
            if (!concurrentRecord.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' has already been used with different request parameters.");
            }
            throw new IdempotencyConflictException(
                    "A concurrent transaction with idempotency key '" + idempotencyKey + "' is already in progress.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String idempotencyKey, int statusCode, Object responseBody, String transactionReference) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }

        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            try {
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseStatusCode(statusCode);
                record.setResponseBody(objectMapper.writeValueAsString(responseBody));
                record.setTransactionReference(transactionReference);
                idempotencyRecordRepository.save(record);
            } catch (Exception e) {
                log.error("Failed to serialize idempotency response body: {}", e.getMessage(), e);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String idempotencyKey, int statusCode, String errorMessage) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }

        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseStatusCode(statusCode);
            record.setResponseBody(errorMessage);
            idempotencyRecordRepository.save(record);
        });
    }
}

