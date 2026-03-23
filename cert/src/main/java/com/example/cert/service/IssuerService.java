package com.example.cert.service;

import com.example.cert.domain.Issuer;
import com.example.cert.repository.IssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuerService {
    
    private final IssuerRepository issuerRepository;
    
    public List<Issuer> getAllIssuers() {
        return issuerRepository.findAll();
    }
    
    public Issuer saveIssuer(Issuer issuer) {
        return issuerRepository.save(issuer);
    }
}
