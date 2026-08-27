-- Datos semilla para poder probar el servicio de inmediato sin infraestructura externa.

INSERT INTO student (student_id, full_name, document_id) VALUES
    ('E001', 'Ana Gomez', '1001234567'),
    ('E002', 'Luis Perez', '1002345678');

INSERT INTO course (course_code, course_name, credits) VALUES
    ('MAT101', 'Calculo I', 3),
    ('FIS201', 'Fisica II', 4),
    ('PRG301', 'Programacion III', 3);

-- E001: matriculas del periodo actual (2026-2) + una historica retirada de un periodo anterior.
INSERT INTO enrollment (enrollment_id, student_id, course_code, term, group_code, status) VALUES
    (NEXT VALUE FOR seq_enrollment, 'E001', 'MAT101', '2026-2', '01', 'ENROLLED');
INSERT INTO enrollment (enrollment_id, student_id, course_code, term, group_code, status) VALUES
    (NEXT VALUE FOR seq_enrollment, 'E001', 'FIS201', '2026-2', '02', 'ENROLLED');
INSERT INTO enrollment (enrollment_id, student_id, course_code, term, group_code, status) VALUES
    (NEXT VALUE FOR seq_enrollment, 'E001', 'MAT101', '2026-1', '01', 'WITHDRAWN');

-- E002: matriculado en el periodo actual, todavia sin notas registradas.
INSERT INTO enrollment (enrollment_id, student_id, course_code, term, group_code, status) VALUES
    (NEXT VALUE FOR seq_enrollment, 'E002', 'PRG301', '2026-2', '01', 'ENROLLED');

-- Notas de las matriculas vigentes de E001 (enrollment_id 1 = MAT101 2026-2, enrollment_id 2 = FIS201 2026-2).
INSERT INTO grade (grade_id, enrollment_id, assessment, score, recorded_date) VALUES
    (NEXT VALUE FOR seq_grade, 1, 'MIDTERM_1', 4.2, '2026-08-20');
INSERT INTO grade (grade_id, enrollment_id, assessment, score, recorded_date) VALUES
    (NEXT VALUE FOR seq_grade, 2, 'MIDTERM_1', 3.5, '2026-08-21');

-- El acompanante A001 tiene asignado a E001, pero NO a E002.
INSERT INTO advisor_assignment (advisor_id, student_id) VALUES
    ('A001', 'E001');
