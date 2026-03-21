package com.example.cert.service;

import com.example.cert.Response.MagazineResponse;
import com.example.cert.domain.Magazine;
import com.example.cert.mapper.MagazineMapper;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.MagazineRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MagazineService {

    private final MagazineRepository magazineRepository;

    public List<MagazineResponse> getAllMagazines() {
        return magazineRepository
                .findAll()
                .stream()
                .map(MagazineMapper::toResponse)
                .toList();
    }

    public MagazineResponse postMagazine(MagazineRequest magazineRequest) {

        Magazine magazine = MagazineMapper.toEntity(magazineRequest);

        magazineRepository.save(magazine);

        return MagazineMapper.toResponse(magazine);
    }
}