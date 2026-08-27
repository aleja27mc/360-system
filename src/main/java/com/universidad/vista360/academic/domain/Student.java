package com.universidad.vista360.academic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class Student {

    @Id
    private String studentId;

    private String fullName;

    private String documentId;

    protected Student() {
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentId() {
        return documentId;
    }
}
