package com.universidad.vista360.academic.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.universidad.vista360.academic.domain.AdvisorAssignmentId;
import com.universidad.vista360.academic.domain.Course;
import com.universidad.vista360.academic.domain.Enrollment;
import com.universidad.vista360.academic.domain.EnrollmentStatus;
import com.universidad.vista360.academic.dto.CoursesAndGradesResponseDTO;
import com.universidad.vista360.academic.dto.EnrolledCourseDTO;
import com.universidad.vista360.academic.dto.GradeDTO;
import com.universidad.vista360.academic.exception.StudentNotFoundException;
import com.universidad.vista360.academic.exception.UnauthorizedAccessException;
import com.universidad.vista360.academic.repository.AdvisorAssignmentRepository;
import com.universidad.vista360.academic.repository.CourseRepository;
import com.universidad.vista360.academic.repository.EnrollmentRepository;
import com.universidad.vista360.academic.repository.GradeRepository;
import com.universidad.vista360.academic.repository.StudentRepository;
import com.universidad.vista360.academic.security.UserPrincipal;

@Service
public class StudentAcademicService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final AdvisorAssignmentRepository advisorAssignmentRepository;
    private final AuditService auditService;
    private final String currentTerm;

    public StudentAcademicService(StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            GradeRepository gradeRepository,
            CourseRepository courseRepository,
            AdvisorAssignmentRepository advisorAssignmentRepository,
            AuditService auditService,
            @Value("${academic.current-term}") String currentTerm) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
        this.advisorAssignmentRepository = advisorAssignmentRepository;
        this.auditService = auditService;
        this.currentTerm = currentTerm;
    }

    @Transactional(readOnly = true)
    public CoursesAndGradesResponseDTO getCoursesAndGrades(String studentId, UserPrincipal caller) {
        String actorId = caller.role() == UserPrincipal.Role.STUDENT ? caller.studentId() : caller.subject();

        try {
            authorize(studentId, caller);
        } catch (UnauthorizedAccessException ex) {
            auditService.record(actorId, caller.role().name(), studentId,
                    AuditService.ACTION_GET_COURSES_AND_GRADES, AuditService.DENIED);
            throw ex;
        }

        if (!studentRepository.existsById(studentId)) {
            auditService.record(actorId, caller.role().name(), studentId,
                    AuditService.ACTION_GET_COURSES_AND_GRADES, AuditService.NOT_FOUND);
            throw new StudentNotFoundException(studentId);
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndTermAndStatus(studentId, currentTerm,
                EnrollmentStatus.ENROLLED);

        Map<String, Course> coursesByCode = courseRepository.findAllById(
                enrollments.stream().map(Enrollment::getCourseCode).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Course::getCourseCode, c -> c));

        List<EnrolledCourseDTO> enrolledCourses = enrollments.stream()
                .map(e -> new EnrolledCourseDTO(
                        e.getCourseCode(),
                        coursesByCode.get(e.getCourseCode()).getCourseName(),
                        e.getGroupCode(),
                        e.getTerm(),
                        coursesByCode.get(e.getCourseCode()).getCredits(),
                        e.getStatus()))
                .toList();

        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getEnrollmentId).toList();
        Map<Long, String> courseCodeByEnrollmentId = enrollments.stream()
                .collect(java.util.stream.Collectors.toMap(Enrollment::getEnrollmentId, Enrollment::getCourseCode));

        List<GradeDTO> currentGrades = gradeRepository.findByEnrollmentIdIn(enrollmentIds).stream()
                .map(g -> new GradeDTO(
                        courseCodeByEnrollmentId.get(g.getEnrollmentId()),
                        currentTerm,
                        g.getAssessment(),
                        g.getScore(),
                        g.getRecordedDate()))
                .toList();

        auditService.record(actorId, caller.role().name(), studentId,
                AuditService.ACTION_GET_COURSES_AND_GRADES, AuditService.ALLOWED);

        return new CoursesAndGradesResponseDTO(studentId, enrolledCourses, currentGrades);
    }

    private void authorize(String studentId, UserPrincipal caller) {
        boolean allowed = switch (caller.role()) {
            case STUDENT -> studentId.equals(caller.studentId());
            case ADVISOR -> advisorAssignmentRepository.existsById(
                    new AdvisorAssignmentId(caller.subject(), studentId));
        };

        if (!allowed) {
            throw new UnauthorizedAccessException(
                    "Caller is not authorized to access student " + studentId);
        }
    }
}
