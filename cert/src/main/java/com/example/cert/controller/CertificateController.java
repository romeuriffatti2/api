package com.example.cert.controller;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.request.CertificateRequest;
import com.example.cert.service.CertificateService;
import com.example.cert.service.RateLimiterService;
import com.example.cert.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/certificate")
@AllArgsConstructor
public class CertificateController {

    private CertificateService certificateService;
    private RateLimiterService rateLimiterService;
    private UserRepository userRepository;

    private Long getUsuarioId(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"))
                .getId();
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public Page<CertificateResponse> getAllCertificates(Pageable pageable) {
        return certificateService.getAllCertificates(pageable);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<Void> createCertificate(@RequestBody CertificateRequest certificateRequest, Principal principal) {
        certificateService.create(certificateRequest, getUsuarioId(principal));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<CertificateResponse> validateCertificate(@PathVariable("code") String code) {
        return ResponseEntity.ok(certificateService.validateCertificate(code));
    }

    @PostMapping("/resend/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<Void> resendCertificate(@PathVariable("code") String code) {
        certificateService.resendEmail(code);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download/{code}")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable("code") String code) {
        byte[] pdf = certificateService.downloadCertificate(code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificado.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    /**
     * Endpoint público — envia todos os certificados vinculados ao e-mail informado.
     * Sempre retorna 200 OK para não revelar se o e-mail está cadastrado (LGPD).
     * Rate limit: 5 requisições por IP a cada 10 minutos.
     */
    @PostMapping("/send-by-email")
    public ResponseEntity<Void> sendCertificatesByEmail(
            @RequestParam String email,
            HttpServletRequest request) {

        String ip = resolveClientIp(request);

        if (!rateLimiterService.isAllowed(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.");
        }

        certificateService.sendCertificatesByEmail(email);
        return ResponseEntity.ok().build();
    }

    /** Extrai o IP real do cliente, considerando proxies reversos. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

