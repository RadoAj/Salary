package com.gestion.salarymanagement.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Deduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;           // Ex: "CNAPS", "OSTIE", "Absence"
    private double amount;

    @ManyToMany(mappedBy = "deductions")
    private List<Payroll> payrolls = new ArrayList<>();
}
