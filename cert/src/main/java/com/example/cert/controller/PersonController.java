package com.example.cert.controller;

import com.example.cert.Response.PersonResponse;
import com.example.cert.request.PersonRequest;
import com.example.cert.service.PersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class PersonController {

    private final PersonService personService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse postPerson(@RequestBody @Valid PersonRequest personRequest) {
        return personService.postPerson(personRequest);
    }

    @GetMapping
    public java.util.List<PersonResponse> getAllPersons() {
        return personService.getAllPersons();
    }
}
