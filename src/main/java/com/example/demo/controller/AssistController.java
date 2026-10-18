package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AssistRequest;
import com.example.demo.dto.AssistResponse;
import com.example.demo.service.AssistService;

@RestController
public class AssistController {

	private final AssistService assistService;

	public AssistController(AssistService assistService) {
		this.assistService = assistService;
	}

	@PostMapping("/notes/{id}/assist")
	public AssistResponse assist(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@RequestBody AssistRequest request) {
		return assistService.assist(Long.valueOf(jwt.getSubject()), id, request);
	}

}
