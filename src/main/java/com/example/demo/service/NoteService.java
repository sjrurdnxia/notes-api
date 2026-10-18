package com.example.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.NoteRequest;
import com.example.demo.dto.NoteResponse;
import com.example.demo.repository.NoteRepository;

@Service
public class NoteService {

	private final NoteRepository repository;

	public NoteService(NoteRepository repository) {
		this.repository = repository;
	}

	public NoteResponse createNote(Long userId, NoteRequest request) {
		// A brand-new note may be empty, so missing fields become ""
		return NoteResponse.from(repository.insertNote(userId, orEmpty(request.title()), orEmpty(request.content())));
	}

	public List<NoteResponse> listNotes(Long userId) {
		return repository.findNotesByUser(userId).stream().map(NoteResponse::from).toList();
	}

	public NoteResponse getNote(Long userId, Long id) {
		return repository.findNote(id, userId).map(NoteResponse::from).orElseThrow(this::notFound);
	}

	public NoteResponse updateNote(Long userId, Long id, NoteRequest request) {
		return repository.updateNote(id, userId, orEmpty(request.title()), orEmpty(request.content()))
				.map(NoteResponse::from)
				.orElseThrow(this::notFound);
	}

	public void deleteNote(Long userId, Long id) {
		if (repository.deleteNote(id, userId) == 0) {
			throw notFound();
		}
	}

	// Same 404 whether the note doesn't exist or belongs to someone else, so ids can't be probed
	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "note not found");
	}

	private static String orEmpty(String value) {
		return value == null ? "" : value;
	}

}
