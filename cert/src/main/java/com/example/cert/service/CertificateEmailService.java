package com.example.cert.service;

import com.example.cert.domain.Certificate;
import com.example.cert.domain.CertificateStatus;
import com.example.cert.repository.CertificateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Sprint 2 — Serviço de disparo de e-mail com PDF em anexo.
 * <p>
 * Responsabilidade única: enviar o certificado por e-mail e atualizar o status
 * de cada Certificate para EMAIL_SENT ou EMAIL_FAILED.
 */
@Slf4j
@Service
public class CertificateEmailService {

    private final JavaMailSender mailSender;
    private final CertificateRepository certificateRepository;
    private final String storagePath;

    public CertificateEmailService(
            JavaMailSender mailSender,
            CertificateRepository certificateRepository,
            @Value("${app.certificate.storage.path}") String storagePath) {
        this.mailSender = mailSender;
        this.certificateRepository = certificateRepository;
        this.storagePath = storagePath;
    }

    /**
     * Envia o PDF salvo no disco para os destinatários de um lote de certificados.
     *
     * @param certificates lista de certificados gerados no batch
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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject("Seu certificado está disponível");
            helper.setText(buildEmailBody(certificate), true);
            helper.addAttachment("certificado.pdf", new FileSystemResource(pdfFile));

            mailSender.send(message);

            certificate.setStatus(CertificateStatus.EMAIL_SENT);
            certificateRepository.save(certificate);

            log.info("E-mail enviado com sucesso para {} (certificado id={})", recipient, certificate.getId());

        } catch (MessagingException e) {
            certificate.setStatus(CertificateStatus.EMAIL_FAILED);
            certificateRepository.save(certificate);
            log.error("Falha ao enviar e-mail para {} (certificado id={}): {}", recipient, certificate.getId(),
                    e.getMessage());
        }
    }

    /**
     * Resolve o e-mail do destinatário: prioriza recipientEmail (campo
     * desnormalizado),
     * depois cai para person.email se existir.
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
     * Template HTML simples do corpo do e-mail.
     * Na Sprint 3, isso pode ser substituído por um template Thymeleaf.
     */
    private String buildEmailBody(Certificate certificate) {
        String name = certificate.getPerson() != null
                ? certificate.getPerson().getName()
                : certificate.getName();

        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Certificado emitido</h2>
                    <p>Olá, <strong>%s</strong>!</p>
                    <p>Seu certificado foi emitido com sucesso. Você pode encontrá-lo em anexo neste e-mail.</p>
                    <p>Para validar a autenticidade do seu certificado, acesse o link abaixo e insira o código:</p>
                    <p><strong>%s</strong></p>
                    <br>
                    <p>Atenciosamente,<br>Equipe da Plataforma</p>
                  </body>
                </html>
                """.formatted(
                name != null ? name : "Prezado(a)",
                certificate.getValidationCode() != null ? certificate.getValidationCode().toString() : "");
    }
}
