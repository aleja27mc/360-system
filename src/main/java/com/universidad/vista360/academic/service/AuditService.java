package com.universidad.vista360.academic.service;

import java.time.LocalDateTime;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.universidad.vista360.academic.domain.AuditLog;
import com.universidad.vista360.academic.repository.AuditLogRepository;
import com.universidad.vista360.academic.web.CorrelationIdFilter;

@Service
public class AuditService {

    public static final String ALLOWED = "ALLOWED";
    public static final String DENIED = "DENIED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ACTION_GET_COURSES_AND_GRADES = "GET_COURSES_AND_GRADES";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actorId, String actorRole, String studentId, String action, String result) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        auditLogRepository.save(new AuditLog(actorId, actorRole, studentId, action, result,
                correlationId, LocalDateTime.now()));
    }
}
