package com.universidad.vista360.academic;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.universidad.vista360.academic.security.JwtService;
import com.universidad.vista360.academic.security.UserPrincipal;

@SpringBootTest
class GenerateTestTokensTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void printSampleTokens() {
        String studentToken = jwtService.generateDemoToken("E001", UserPrincipal.Role.STUDENT, "E001",
                Duration.ofHours(8));
        String otherStudentToken = jwtService.generateDemoToken("E002", UserPrincipal.Role.STUDENT, "E002",
                Duration.ofHours(8));
        String advisorToken = jwtService.generateDemoToken("A001", UserPrincipal.Role.ADVISOR, null,
                Duration.ofHours(8));
        String unknownStudentToken = jwtService.generateDemoToken("E999", UserPrincipal.Role.STUDENT, "E999",
                Duration.ofHours(8));

        System.out.println("\n=== SAMPLE TOKENS (HS256, demo secret) ===");
        System.out.println("STUDENT E001 (own data - should get 200 on /api/v1/students/E001/courses):");
        System.out.println(studentToken);
        System.out.println("\nSTUDENT E002 (used to test cross-student access - should get 403 on .../E001/courses):");
        System.out.println(otherStudentToken);
        System.out.println("\nADVISOR A001 (assigned to E001 - should get 200 on E001, 403 on E002):");
        System.out.println(advisorToken);
        System.out.println(
                "\nSTUDENT E999 (claims an id that does not exist in this service's store - should get 404 on .../E999/courses):");
        System.out.println(unknownStudentToken);
        System.out.println("===========================================\n");
    }
}
