package com.gestion.salarymanagement.dto;

import lombok.Data;

@Data
public class BonusDTO {
    private Long id;
    private String type;
    private double amount;
}
