package com.universidad.vista360.academic;

import com.universidad.vista360.academic.security.JwtService;
import com.universidad.vista360.academic.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudentControllerAuthorizationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void studentCanReadOwnData() {
        String token = studentToken("E001", "E001");
        ResponseEntity<String> response = get("/api/v1/students/E001/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"studentId\":\"E001\"").contains("MAT101");
    }

    @Test
    void studentCannotReadAnotherStudent() {
        String token = studentToken("E002", "E002");
        ResponseEntity<String> response = get("/api/v1/students/E001/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void advisorCanReadAssignedStudent() {
        String token = advisorToken("A001");
        ResponseEntity<String> response = get("/api/v1/students/E001/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void advisorCannotReadUnassignedStudent() {
        String token = advisorToken("A001");
        ResponseEntity<String> response = get("/api/v1/students/E002/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unknownStudentReturnsNotFound() {
        String token = studentToken("E999", "E999");
        ResponseEntity<String> response = get("/api/v1/students/E999/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestWithoutTokenIsRejected() {
        ResponseEntity<String> response = get("/api/v1/students/E001/courses", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void studentWithNoGradesYetGetsEmptyArray() {
        String token = studentToken("E002", "E002");
        ResponseEntity<String> response = get("/api/v1/students/E002/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"currentGrades\":[]");
    }

    @Test
    void studentIdExceedingMaxLengthIsRejected() {
        String token = studentToken("E001", "E001");
        String tooLong = "E".repeat(25);
        ResponseEntity<String> response = get("/api/v1/students/" + tooLong + "/courses", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String studentToken(String subject, String studentId) {
        return jwtService.generateDemoToken(subject, UserPrincipal.Role.STUDENT, studentId, Duration.ofHours(1));
    }

    private String advisorToken(String subject) {
        return jwtService.generateDemoToken(subject, UserPrincipal.Role.ADVISOR, null, Duration.ofHours(1));
    }

    private ResponseEntity<String> get(String path, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange("http://localhost:" + port + path,
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
