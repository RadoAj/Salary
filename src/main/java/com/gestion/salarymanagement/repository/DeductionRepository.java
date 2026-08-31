package com.gestion.salarymanagement.repository;

import com.gestion.salarymanagement.entity.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DeductionRepository extends JpaRepository<Deduction, Long> {
}
