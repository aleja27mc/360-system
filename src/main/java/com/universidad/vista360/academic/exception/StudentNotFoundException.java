package com.universidad.vista360.academic.exception;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(String studentId) {
        super("Student not found: " + studentId);
    }
}
