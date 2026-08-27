package com.universidad.vista360.academic.controller;

import com.universidad.vista360.academic.dto.CoursesAndGradesResponseDTO;
import com.universidad.vista360.academic.security.UserPrincipal;
import com.universidad.vista360.academic.service.StudentAcademicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Student academic data")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class StudentController {

    private static final String STUDENT_ID_PATTERN = "^[A-Za-z0-9]{1,20}$";

    private final StudentAcademicService studentAcademicService;

    public StudentController(StudentAcademicService studentAcademicService) {
        this.studentAcademicService = studentAcademicService;
    }

    @GetMapping("/{studentId}/courses")
    @Operation(
            summary = "Get a student's currently enrolled courses and their current grades",
            description = "A STUDENT may only query their own studentId. An ADVISOR may only query "
                    + "students explicitly assigned to them (validated server-side, never trusted from the token).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK, including students with no active enrollments (empty arrays)"),
                    @ApiResponse(responseCode = "400", description = "studentId does not match the expected format"),
                    @ApiResponse(responseCode = "403", description = "Caller not authorized for this studentId"),
                    @ApiResponse(responseCode = "404", description = "studentId does not exist")
            }
    )
    public ResponseEntity<CoursesAndGradesResponseDTO> getCoursesAndGrades(
            @PathVariable @Pattern(regexp = STUDENT_ID_PATTERN) String studentId,
            @AuthenticationPrincipal UserPrincipal caller) {
        return ResponseEntity.ok(studentAcademicService.getCoursesAndGrades(studentId, caller));
    }
}
