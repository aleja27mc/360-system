package com.universidad.vista360.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AdvisorAssignmentId implements Serializable {

    @Column(name = "advisor_id")
    private String advisorId;

    @Column(name = "student_id")
    private String studentId;

    protected AdvisorAssignmentId() {
    }

    public AdvisorAssignmentId(String advisorId, String studentId) {
        this.advisorId = advisorId;
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvisorAssignmentId that)) return false;
        return Objects.equals(advisorId, that.advisorId) && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(advisorId, studentId);
    }
}
