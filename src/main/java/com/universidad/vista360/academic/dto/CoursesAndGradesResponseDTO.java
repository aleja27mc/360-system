package com.universidad.vista360.academic.dto;

import java.util.List;

public record CoursesAndGradesResponseDTO(
        String studentId,
        List<EnrolledCourseDTO> enrolledCourses,
        List<GradeDTO> currentGrades
) {
}
