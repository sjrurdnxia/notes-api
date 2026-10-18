package com.example.demo.dto;

import java.time.Instant;

import com.example.demo.entity.Note;

public record NoteResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {

	public static NoteResponse from(Note note) {
		return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt(),
				note.getUpdatedAt());
	}

}
