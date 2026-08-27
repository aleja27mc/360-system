package com.universidad.vista360.academic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GradeDTO(
        String courseCode,
        String term,
        String assessment,
        BigDecimal score,
        LocalDate recordedDate
) {
}
