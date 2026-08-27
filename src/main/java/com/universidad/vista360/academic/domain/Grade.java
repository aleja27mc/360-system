package com.universidad.vista360.academic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grade")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_grade")
    @SequenceGenerator(name = "seq_grade", sequenceName = "seq_grade", allocationSize = 1)
    private Long gradeId;

    private Long enrollmentId;

    private String assessment;

    private BigDecimal score;

    private LocalDate recordedDate;

    protected Grade() {
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getAssessment() {
        return assessment;
    }

    public BigDecimal getScore() {
        return score;
    }

    public LocalDate getRecordedDate() {
        return recordedDate;
    }
}
