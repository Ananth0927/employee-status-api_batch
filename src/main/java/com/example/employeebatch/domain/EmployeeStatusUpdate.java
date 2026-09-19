package com.example.employeebatch.domain;

import java.time.LocalDate;

public record EmployeeStatusUpdate(
        Long employeeId,
        String status,
        LocalDate statusDate
) {
}
