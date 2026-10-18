package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.NameRequest;
import com.example.demo.dto.PersonResponse;
import com.example.demo.service.PersonService;

@RestController
@RequestMapping("/my/first/api")
public class NameController {

	private final PersonService personService;

	public NameController(PersonService personService) {
		this.personService = personService;
	}

	@PostMapping("/add")
	public PersonResponse addName(@RequestBody NameRequest request) {
		return personService.addPerson(request);
	}

	@GetMapping("/{id}")
	public PersonResponse getPerson(@PathVariable Long id) {
		return personService.getPerson(id);
	}

}
