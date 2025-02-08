package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryAuditLogRepositoryImpl implements AuditLogRepository {
    private final Map<Long, AuditLog> auditLogs = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public AuditLog save(AuditLog auditLog) {
        auditLog.setId(idGenerator.getAndIncrement());
        auditLogs.put(auditLog.getId(), auditLog);
        return auditLog;
    }

    @Override
    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId) {
        return auditLogs.values().stream()
                .filter(log -> log.getEntityType().equals(entityType) && log.getEntityId().equals(entityId))
                .collect(Collectors.toList());
    }
} 