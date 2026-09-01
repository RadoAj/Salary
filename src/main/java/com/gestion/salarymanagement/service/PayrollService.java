package com.gestion.salarymanagement.service;

import com.gestion.salarymanagement.dto.BonusDTO;
import com.gestion.salarymanagement.dto.DeductionDTO;
import com.gestion.salarymanagement.dto.PayrollDTO;
import com.gestion.salarymanagement.entity.*;
import com.gestion.salarymanagement.repository.BonusRepository;
import com.gestion.salarymanagement.repository.DeductionRepository;
import com.gestion.salarymanagement.repository.EmployeeRepository;
import com.gestion.salarymanagement.repository.PayrollRepository;
import com.gestion.salarymanagement.repository.SalaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private SalaryRepository salaryRepository;
    @Autowired
    private BonusRepository bonusRepository;
    @Autowired
    private DeductionRepository deductionRepository;
    @Autowired
    private PdfExportService pdfExportService;
    @Autowired
    private EmailService emailService;

    public List<PayrollDTO> getAllPayrolls() {
        return payrollRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PayrollDTO getPayrollById(Long id){
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paie introuvable avec l'id: " + id));
        return getPayrollDTO(payroll);
    }

    public boolean existsById(Long id){
        return payrollRepository.existsById(id);
    }

    public Payroll createPayroll(PayrollDTO dto) {
        try {
            // Vérification de l'employé d'abord
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé (ID: " + dto.getEmployeeId() + ")"));

            // Vérification du salaire
            Salary salary = salaryRepository.findByEmployeeId(dto.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Aucun salaire trouvé pour " +
                            employee.getName() + " " + employee.getFirstName()));

            // Vérification si une fiche existe déjà
            payrollRepository.findBySalaryId(salary.getId()).ifPresent(p -> {
                throw new IllegalStateException(
                        "Une fiche de paie existe déjà pour l'employé " +
                                employee.getName() + " " + employee.getFirstName()
                );
            });

            Payroll payroll = new Payroll();
            payroll.setEmployee(employee);
            payroll.setSalary(salary);
            updatePayrollFields(payroll, dto);

            return payrollRepository.save(payroll);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Solution de repli si la vérification précédente a échoué
            Employee employee = employeeRepository.findById(dto.getEmployeeId()).orElse(null);
            String employeeName = (employee != null)
                    ? employee.getName() + " " + employee.getFirstName()
                    : "cet employé";

            throw new IllegalStateException(
                    "Une fiche de paie existe déjà pour " + employeeName
            );
        }
    }

    public Payroll updatePayroll(Long id, PayrollDTO dto) {
        try {
            Payroll existingPayroll = payrollRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Paie introuvable avec l'id: " + id));

            Employee newEmployee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employé introuvable"));

            // Modifiez cette partie pour inclure le nom de l'employé dans le message d'erreur
            Salary newSalary = salaryRepository.findByEmployeeId(dto.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aucun salaire trouvé pour " + newEmployee.getName() + " " + newEmployee.getFirstName()
                    ));

            // Vérification si le salaire change et existe déjà dans une autre fiche
            if (!existingPayroll.getSalary().getId().equals(newSalary.getId())) {
                payrollRepository.findBySalaryId(newSalary.getId()).ifPresent(p -> {
                    throw new IllegalStateException(
                            "L'employé " + newEmployee.getName() + " " + newEmployee.getFirstName() +
                                    " a déjà une fiche de paie existante"
                    );
                });
            }

            updatePayrollFields(existingPayroll, dto);
            return payrollRepository.save(existingPayroll);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            Employee employee = employeeRepository.findById(dto.getEmployeeId()).orElse(null);
            String employeeName = (employee != null)
                    ? employee.getName()  + " " + employee.getFirstName()
                    : "cet employé";

            throw new IllegalStateException(
                    "L'employé " + employeeName + " a déjà une fiche de paie existante"
            );
        }
    }

    public void deletePayroll(Long id){
        payrollRepository.deleteById(id);
    }

    public byte[] generatePayrollPdf(Long payrollId) {
        PayrollDTO dto = getPayrollById(payrollId);
        return pdfExportService.generatePayrollPdf(dto);
    }

    public void sendPayrollByEmail(Long payrollId) {
        PayrollDTO dto = getPayrollById(payrollId);
        byte[] pdf = pdfExportService.generatePayrollPdf(dto);

        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", dto.getEmployeeName());
        variables.put("periodStart", dto.getPeriodStartFormatted());
        variables.put("periodEnd", dto.getPeriodEndFormatted());

        emailService.sendPayrollEmailWithTemplate(
                dto.getEmployeeEmail(),
                "Fiche de paie - " + dto.getPeriodEndFormatted(),
                "payroll-mail", // chemin relatif au dossier templates/
                variables,
                pdf,
                "fiche_paie_" + payrollId + ".pdf"
        );
    }

    public List<String> sendAllPayrollsByEmail() {
        var payrolls = getAllPayrolls();
        List<String> errors = new ArrayList<>();

        for (PayrollDTO dto : payrolls) {
            try {
                sendPayrollByEmail(dto.getId());
            } catch (Exception e) {
                var message = "Erreur pour " + dto.getEmployeeName() + " (ID " + dto.getId() + ") : " + e.getMessage();
                System.err.println(message);
                errors.add(message);
            }
        }
        return errors;
    }

    public PayrollDTO toDTO(Payroll payroll) {
        return getPayrollDTO(payroll);
    }

    private void updatePayrollFields(Payroll payroll, PayrollDTO dto) {
        payroll.setPeriodStart(dto.getPeriodStart());
        payroll.setPeriodEnd(dto.getPeriodEnd());

        List<Bonus> bonuses = dto.getBonuses() != null
                ? dto.getBonuses().stream()
                .map(b -> bonusRepository.findById(b.getId())
                        .orElseThrow(() -> new RuntimeException("Bonus introuvable : id=" + b.getId())))
                .collect(Collectors.toList())
                : new ArrayList<>();
        payroll.setBonuses(bonuses);

        List<Deduction> deductions = dto.getDeductions() != null
                ? dto.getDeductions().stream()
                .map(d -> deductionRepository.findById(d.getId())
                        .orElseThrow(() -> new RuntimeException("Déduction introuvable : id=" + d.getId())))
                .collect(Collectors.toList())
                : new ArrayList<>();
        payroll.setDeductions(deductions);
    }

    private PayrollDTO getPayrollDTO(Payroll payroll) {
        List<BonusDTO> bonuses = payroll.getBonuses() != null
                ? payroll.getBonuses().stream()
                .map(BonusService::toBonusDTO)
                .collect(Collectors.toList())
                : new ArrayList<>();

        List<DeductionDTO> deductions = payroll.getDeductions() != null
                ? payroll.getDeductions().stream()
                .map(DeductionService::toDeductionDTO)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return getPayrollDTO(payroll, bonuses, deductions);
    }

    private PayrollDTO getPayrollDTO(Payroll payroll, List<BonusDTO> bonuses, List<DeductionDTO> deductions) {
        PayrollDTO dto = new PayrollDTO();
        dto.setId(payroll.getId());
        dto.setPeriodStart(payroll.getPeriodStart());
        dto.setPeriodEnd(payroll.getPeriodEnd());
        dto.setSalary(payroll.getSalary());
        dto.setGrossSalary(payroll.getGrossSalary());
        dto.setNetSalary(payroll.getNetSalary());
        dto.setBonuses(bonuses);
        dto.setDeductions(deductions);
        dto.setEmployeeId(payroll.getEmployee().getId());
        dto.setEmployeeName(payroll.getEmployee().getName() + " " + payroll.getEmployee().getFirstName());
        dto.setEmployeeEmail(payroll.getEmployee().getEmail());
        return dto;
    }
}