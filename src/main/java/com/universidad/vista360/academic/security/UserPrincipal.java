package com.universidad.vista360.academic.security;

public record UserPrincipal(String subject, Role role, String studentId) {

    public enum Role {
        STUDENT,
        ADVISOR
    }
}
