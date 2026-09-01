package com.gestion.salarymanagement.service;

import com.com.gestion.salarymanagement.dto.PayrollDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfExportService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    private String loadLogoAsBase64() {
        try {
            var resource = new ClassPathResource("static/images/logo-transparent.png");
            byte[] bytes = resource.getInputStream().readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du logo", e);
        }
    }

    public byte[] generatePayrollPdf(PayrollDTO dto) {
        var context = new Context();
        context.setVariable("payroll", dto);
        context.setVariable("today", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("logo", loadLogoAsBase64());

        var html = templateEngine.process("payroll-report", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();  
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur PDF : " + e.getMessage(), e);
        }
    }
}
```


#### 5. `service/EmailService.java` (version complète avec pièce jointe PDF)

> Remplace le fichier Dev 1 par cette version complète.

```java
package com.mycompany.salary_management.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;
    public void sendPayrollEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables, byte[] pdfAttachment, String fileName) {
        try {
            // Construire le contenu HTML de l’e-mail
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            // Création du message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("ton.email@gmail.com", "No Reply - Paie RH");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment(fileName, new ByteArrayResource(pdfAttachment));

            mailSender.send(message);

        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Échec d'envoi de l'e-mail à " + to + " : " + e.getMessage(), e);
        }
    }
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@yourdomain.com", "No Reply,Salary Management");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoye de l'email: " + e.getMessage());
        }
    }
}