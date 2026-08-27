package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {
}
