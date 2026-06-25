package com.example.cert.controller;

import com.example.cert.Response.PersonResponse;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.PersonRequest;
import com.example.cert.request.PersonUpdateRequest;
import com.example.cert.service.PersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/persons")
@AllArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final UserRepository userRepository;

    private Long getUsuarioId(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"))
                .getId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse postPerson(@RequestBody @Valid PersonRequest personRequest, Principal principal) {
        return personService.postPerson(personRequest, getUsuarioId(principal));
    }

    @GetMapping
    public List<PersonResponse> getAllPersons(Principal principal) {
        return personService.getAllPersons(getUsuarioId(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable Long id, Principal principal) {
        personService.deletePerson(id, getUsuarioId(principal));
    }

    @PutMapping("/{id}")
    public PersonResponse updatePerson(
            @PathVariable Long id,
            @RequestBody @Valid PersonUpdateRequest request,
            Principal principal) {
        return personService.updatePerson(id, request, getUsuarioId(principal));
    }

    @PutMapping("/{id}/reactivate")
    public PersonResponse reactivatePerson(
            @PathVariable Long id,
            @RequestBody @Valid PersonUpdateRequest request,
            Principal principal) {
        return personService.reactivatePerson(id, request, getUsuarioId(principal));
    }
}
