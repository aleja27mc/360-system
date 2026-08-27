package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByEnrollmentIdIn(List<Long> enrollmentIds);
}
