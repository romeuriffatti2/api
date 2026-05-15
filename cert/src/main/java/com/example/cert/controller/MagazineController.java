package com.example.cert.controller;

import com.example.cert.Response.MagazineResponse;

import com.example.cert.request.MagazineRequest;
import com.example.cert.service.MagazineService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/magazines")
@AllArgsConstructor
public class MagazineController {

    private final MagazineService magazineService;

    @GetMapping
    public List<MagazineResponse> getMagazines() {
        return magazineService.getAllMagazines();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MagazineResponse postMagazine(@RequestBody MagazineRequest magazineRequest) {
        return magazineService.postMagazine(magazineRequest);
    }
}
