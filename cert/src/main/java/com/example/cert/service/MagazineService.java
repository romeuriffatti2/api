package com.example.cert.service;

import com.example.cert.Response.MagazineResponse;
import com.example.cert.domain.Magazine;
import com.example.cert.domain.Usuario;
import com.example.cert.mapper.MagazineMapper;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.MagazineRequest;
import com.example.cert.service.templates.TemplateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final TemplateService templateService;

    public List<MagazineResponse> getMyMagazines(Usuario owner) {
        return magazineRepository
                .findByOwner(owner)
                .stream()
                .map(MagazineMapper::toResponse)
                .toList();
    }

    @Transactional
    public MagazineResponse postMagazine(MagazineRequest magazineRequest, Usuario owner) {

        Magazine magazine = MagazineMapper.toEntity(magazineRequest);
        magazine.setOwner(owner);

        Magazine saved = magazineRepository.save(magazine);

        // Clona os templates padrões do sistema diretamente vinculados a esta nova revista
        templateService.cloneTemplatesForMagazine(saved);

        return MagazineMapper.toResponse(saved);
    }
}