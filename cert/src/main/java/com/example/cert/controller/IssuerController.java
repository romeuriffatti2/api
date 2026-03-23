package com.example.cert.controller;

import com.example.cert.domain.Issuer;
import com.example.cert.service.IssuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issuers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IssuerController {

    private final IssuerService issuerService;

    @GetMapping
    public ResponseEntity<List<Issuer>> getAllIssuers() {
        return ResponseEntity.ok(issuerService.getAllIssuers());
    }

    @PostMapping
    public ResponseEntity<Issuer> createIssuer(@RequestBody Issuer issuer) {
        return ResponseEntity.ok(issuerService.saveIssuer(issuer));
    }
}
