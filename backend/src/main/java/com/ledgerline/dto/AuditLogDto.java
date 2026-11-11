package com.ledgerline.dto;

import com.ledgerline.entity.AuditLog;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.AuditStatus;
import java.time.OffsetDateTime;

public class AuditLogDto {

    private Long id;
    private Long actorUserId;
    private String actorUsername;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private AuditStatus status;
    private String ipAddress;
    private String errorMessage;
    private String metadata;
    private OffsetDateTime timestamp;

    public AuditLogDto() {}

    public AuditLogDto(AuditLog log) {
        if (log != null) {
            this.id = log.getId();
            this.actorUserId = log.getActorUserId();
            this.actorUsername = log.getActorUsername();
            this.action = log.getAction();
            this.entityType = log.getEntityType();
            this.entityId = log.getEntityId();
            this.status = log.getStatus();
            this.ipAddress = log.getIpAddress();
            this.errorMessage = log.getErrorMessage();
            this.metadata = log.getMetadata();
            this.timestamp = log.getTimestamp();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public void setStatus(AuditStatus status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

