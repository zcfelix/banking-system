package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.AuditLog;
import java.util.List;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
} 