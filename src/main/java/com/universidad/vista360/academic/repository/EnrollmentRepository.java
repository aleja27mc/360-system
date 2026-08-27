package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.Enrollment;
import com.universidad.vista360.academic.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentIdAndTermAndStatus(String studentId, String term, EnrollmentStatus status);
}
