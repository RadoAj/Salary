package com.gestion.salarymanagement.repository;

import com.gestion.salarymanagement.entity.Bonus;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BonusRepository extends JpaRepository<Bonus, Long> {
}
