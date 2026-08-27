package com.universidad.vista360.academic.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "advisor_assignment")
public class AdvisorAssignment {

    @EmbeddedId
    private AdvisorAssignmentId id;

    protected AdvisorAssignment() {
    }

    public AdvisorAssignmentId getId() {
        return id;
    }
}
