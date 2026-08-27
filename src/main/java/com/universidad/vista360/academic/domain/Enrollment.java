package com.universidad.vista360.academic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "enrollment")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_enrollment")
    @SequenceGenerator(name = "seq_enrollment", sequenceName = "seq_enrollment", allocationSize = 1)
    private Long enrollmentId;

    private String studentId;

    private String courseCode;

    private String term;

    private String groupCode;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    protected Enrollment() {
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTerm() {
        return term;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }
}
