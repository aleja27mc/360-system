package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
