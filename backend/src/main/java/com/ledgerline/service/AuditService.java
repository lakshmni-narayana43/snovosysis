package com.ledgerline.service;

import com.ledgerline.dto.AuditLogDto;
import com.ledgerline.entity.AuditLog;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.AuditStatus;
import com.ledgerline.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(Long actorUserId, String actorUsername, AuditAction action,
                            String entityType, String entityId, AuditStatus status,
                            String ipAddress, String errorMessage, String metadata) {
        try {
            AuditLog auditLog = new AuditLog(
                    actorUserId,
                    actorUsername,
                    action,
                    entityType,
                    entityId,
                    status,
                    ipAddress,
                    errorMessage,
                    metadata
            );
            auditLogRepository.save(auditLog);
            log.info("AUDIT EVENT: user='{}' action='{}' entity='{}:{}' status='{}'",
                    actorUsername, action, entityType, entityId, status);
        } catch (Exception e) {
            log.error("Failed to write audit log entry: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(AuditLogDto::new);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogsByActor(Long actorUserId, Pageable pageable) {
        return auditLogRepository.findByActorUserId(actorUserId, pageable)
                .map(AuditLogDto::new);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(AuditLogDto::new);
    }
}

