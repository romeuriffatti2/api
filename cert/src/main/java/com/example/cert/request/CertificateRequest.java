package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CertificateRequest {

    private List<CertificateItemRequest> certificates;

    private Long magazineId;
}
