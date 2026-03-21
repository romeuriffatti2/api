package com.example.cert.controller;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.request.CertificateRequest;
import com.example.cert.service.CertificateService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
@AllArgsConstructor
@CrossOrigin(origins = "*") // Added for frontend access
public class CertificateController {

    private CertificateService certificateService;

    @GetMapping("/list")
    public Page<CertificateResponse> getAllCertificates(Pageable pageable) {
        return certificateService.getAllCertificates(pageable);
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> createCertificate(@RequestBody CertificateRequest certificateRequest) {
        byte[] pdf = certificateService.create(certificateRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificados.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
    @GetMapping("/validate/{code}")
    public ResponseEntity<CertificateResponse> validateCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateService.validateCertificate(code));
    }
}
