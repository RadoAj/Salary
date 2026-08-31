package com.gestion.salarymanagement.dto;

import lombok.Data;

@Data
public class SalaryDTO {
    Long id;
    double baseSalary;
    Long employeeId;
}
