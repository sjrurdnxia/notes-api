package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.NoteRequest;
import com.example.demo.dto.NoteResponse;
import com.example.demo.service.NoteService;

@RestController
@RequestMapping("/notes")
public class NoteController {

	private final NoteService noteService;

	public NoteController(NoteService noteService) {
		this.noteService = noteService;
	}

	@PostMapping
	public NoteResponse createNote(@AuthenticationPrincipal Jwt jwt, @RequestBody NoteRequest request) {
		return noteService.createNote(userId(jwt), request);
	}

	@GetMapping
	public List<NoteResponse> listNotes(@AuthenticationPrincipal Jwt jwt) {
		return noteService.listNotes(userId(jwt));
	}

	@GetMapping("/{id}")
	public NoteResponse getNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return noteService.getNote(userId(jwt), id);
	}

	@PutMapping("/{id}")
	public NoteResponse updateNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@RequestBody NoteRequest request) {
		return noteService.updateNote(userId(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		noteService.deleteNote(userId(jwt), id);
	}

	// The login token's subject is the user's id (set in AuthService)
	private static Long userId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
