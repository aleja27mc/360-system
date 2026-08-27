package com.universidad.vista360.academic.repository;

import com.universidad.vista360.academic.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {
}
