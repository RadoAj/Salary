package com.gestion.salarymanagement.repository;

import com.gestion.salarymanagement.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Optional<Payroll> findBySalaryId(Long salaryId);
}