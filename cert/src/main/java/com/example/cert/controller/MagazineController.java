package com.example.cert.controller;

import com.example.cert.Response.MagazineResponse;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.MagazineRequest;
import com.example.cert.service.MagazineService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    private Usuario resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public List<MagazineResponse> getMagazines(@AuthenticationPrincipal UserDetails principal) {
        return magazineService.getMyMagazines(resolveUser(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public MagazineResponse postMagazine(@RequestBody MagazineRequest magazineRequest,
            @AuthenticationPrincipal UserDetails principal) {
        return magazineService.postMagazine(magazineRequest, resolveUser(principal));
    }
}
