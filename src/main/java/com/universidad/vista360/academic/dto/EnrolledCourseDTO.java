package com.universidad.vista360.academic.dto;

import com.universidad.vista360.academic.domain.EnrollmentStatus;

public record EnrolledCourseDTO(
        String courseCode,
        String courseName,
        String groupCode,
        String term,
        Integer credits,
        EnrollmentStatus status
) {
}
