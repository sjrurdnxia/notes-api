package com.example.demo.dto;

// text: the note as currently typed (may be unsaved); cursor: caret position, defaults to the end
public record AssistRequest(String text, Integer cursor) {
}
