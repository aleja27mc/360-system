package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.AdvisorAssignment;
import com.universidad.vista360.academic.domain.AdvisorAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorAssignmentRepository extends JpaRepository<AdvisorAssignment, AdvisorAssignmentId> {
}
