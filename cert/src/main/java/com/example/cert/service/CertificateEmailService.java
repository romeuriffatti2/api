package com.example.cert.service;

import com.example.cert.domain.Certificate;
import com.example.cert.domain.CertificateStatus;
import com.example.cert.domain.Magazine;
import com.example.cert.repository.CertificateRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Serviço de disparo de e-mail com PDF em anexo e SMTP dinâmico por Revista.
 */
@Slf4j
@Service
public class CertificateEmailService {

    private final CertificateRepository certificateRepository;
    private final String storagePath;

    public CertificateEmailService(
            CertificateRepository certificateRepository,
            @org.springframework.beans.factory.annotation.Value("${app.certificate.storage.path}") String storagePath) {
        this.certificateRepository = certificateRepository;
        this.storagePath = storagePath;
    }

    /**
     * Instancia dinamicamente o JavaMailSender com as credenciais SMTP da revista
     * associada.
     */
    private JavaMailSender getMailSenderForMagazine(Magazine magazine) {
        if (magazine.getEmail() == null || magazine.getEmailPassword() == null
                || magazine.getEmailPassword().isBlank()) {
            throw new IllegalStateException(
                    "A revista '" + magazine.getName() + "' não possui credenciais SMTP (e-mail e senha) cadastradas.");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com"); // padrão Google SMTP
        mailSender.setPort(587);
        mailSender.setUsername(magazine.getEmail());
        mailSender.setPassword(magazine.getEmailPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }

    /**
     * Envia o PDF salvo no disco para os destinatários de um lote de certificados.
     */
    @Async
    public void sendBatch(List<Certificate> certificates) {
        for (Certificate certificate : certificates) {
            String recipient = resolveRecipient(certificate);
            if (recipient == null || recipient.isBlank()) {
                log.warn("Certificado id={} sem e-mail de destinatário. Skipping.", certificate.getId());
                continue;
            }
            send(certificate, recipient);
        }
    }

    /**
     * Tenta reenviar o certificado para o destinatário associado.
     */
    @Async
    public void resend(Certificate certificate) {
        String recipient = resolveRecipient(certificate);
        if (recipient == null || recipient.isBlank()) {
            log.warn("Certificado id={} sem e-mail de destinatário. Não é possível reenviar.", certificate.getId());
            return;
        }
        send(certificate, recipient);
    }

    /**
     * Envia o e-mail para um único certificado e persiste o novo status.
     */
    private void send(Certificate certificate, String recipient) {
        try {
            File pdfFile = new File(storagePath, certificate.getValidationCode().toString() + ".pdf");
            if (!pdfFile.exists()) {
                log.error("Arquivo PDF não encontrado para o certificado id={}: {}", certificate.getId(),
                        pdfFile.getAbsolutePath());
                return;
            }

            Magazine magazine = certificate.getMagazine();
            if (magazine == null) {
                throw new IllegalStateException(
                        "O certificado id=" + certificate.getId() + " não possui revista associada.");
            }

            // Instancia o MailSender dinâmico da revista
            JavaMailSender dynamicMailSender = getMailSenderForMagazine(magazine);

            MimeMessage message = dynamicMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(magazine.getEmail()); // O remetente passa a ser o e-mail da própria revista
            helper.setTo(recipient);
            helper.setSubject("Seu certificado da revista " + magazine.getName() + " está disponível");
            helper.setText(buildEmailBody(certificate), true);
            helper.addAttachment("certificado.pdf", new FileSystemResource(pdfFile));

            dynamicMailSender.send(message);

            certificate.setStatus(CertificateStatus.EMAIL_SENT);
            certificateRepository.save(certificate);

            log.info("E-mail enviado com sucesso de {} para {} (certificado id={})", magazine.getEmail(), recipient,
                    certificate.getId());

        } catch (Exception e) {
            certificate.setStatus(CertificateStatus.EMAIL_FAILED);
            certificateRepository.save(certificate);
            log.error("Falha ao enviar e-mail para {} (certificado id={}): {}", recipient, certificate.getId(),
                    e.getMessage());
        }
    }

    /**
     * Resolve o e-mail do destinatário.
     */
    private String resolveRecipient(Certificate certificate) {
        if (certificate.getRecipientEmail() != null && !certificate.getRecipientEmail().isBlank()) {
            return certificate.getRecipientEmail();
        }
        if (certificate.getPerson() != null && certificate.getPerson().getEmail() != null) {
            return certificate.getPerson().getEmail();
        }
        return null;
    }

    /**
     * Template HTML do corpo do e-mail.
     */
    private String buildEmailBody(Certificate certificate) {
        String name = certificate.getPerson() != null
                ? certificate.getPerson().getName()
                : certificate.getName();
        String magazineName = certificate.getMagazine() != null
                ? certificate.getMagazine().getName()
                : "Revista";

        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Certificado emitido</h2>
                    <p>Olá, <strong>%s</strong>!</p>
                    <p>Seu certificado relacionado à revista <strong>%s</strong> foi emitido com sucesso e está em anexo.</p>
                    <p>Para validar a autenticidade do seu certificado, acesse o link de validação e insira o código:</p>
                    <p><strong>%s</strong></p>
                    <br>
                    <p>Atenciosamente,<br>Equipe da %s</p>
                  </body>
                </html>
                """
                .formatted(
                        name != null ? name : "Prezado(a)",
                        magazineName,
                        certificate.getValidationCode() != null ? certificate.getValidationCode().toString() : "",
                        magazineName);
    }
}
