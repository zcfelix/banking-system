package com.hsbc.banking.transaction.model;

import java.time.LocalDateTime;

public class AuditLog {
    private Long id;
    private String operation;
    private String entityType;
    private String entityId;
    private String details;
    private LocalDateTime createdAt;

    public AuditLog(String operation, String entityType, String entityId, String details) {
        this.operation = operation;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }
} 