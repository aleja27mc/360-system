package com.universidad.vista360.academic;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.universidad.vista360.academic.repository.AuditLogRepository;
import com.universidad.vista360.academic.security.UserPrincipal;
import com.universidad.vista360.academic.service.StudentAcademicService;

@SpringBootTest
class AuditLoggingIT {

    @Autowired
    private StudentAcademicService studentAcademicService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void allowedReadIsAudited() {
        long before = auditLogRepository.count();

        studentAcademicService.getCoursesAndGrades("E001",
                new UserPrincipal("E001", UserPrincipal.Role.STUDENT, "E001"));

        long after = auditLogRepository.count();
        System.out.println("audit_log count before=" + before + " after=" + after);
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void deniedReadIsAudited() {
        long before = auditLogRepository.count();

        try {
            studentAcademicService.getCoursesAndGrades("E001",
                    new UserPrincipal("E002", UserPrincipal.Role.STUDENT, "E002"));
        } catch (Exception expected) {
        }

        long after = auditLogRepository.count();
        System.out.println("audit_log count before=" + before + " after=" + after);
        assertThat(after).isEqualTo(before + 1);
    }
}
