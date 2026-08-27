package com.universidad.vista360.academic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_audit_log")
    @SequenceGenerator(name = "seq_audit_log", sequenceName = "seq_audit_log", allocationSize = 1)
    private Long auditId;

    private String actorId;
    private String actorRole;
    private String studentId;
    private String action;
    private String result;
    private String correlationId;
    private LocalDateTime occurredAt;

    protected AuditLog() {
    }

    public AuditLog(String actorId, String actorRole, String studentId, String action,
                     String result, String correlationId, LocalDateTime occurredAt) {
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.studentId = studentId;
        this.action = action;
        this.result = result;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }
}
