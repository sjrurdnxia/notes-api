package com.example.demo.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import com.anthropic.models.beta.messages.StructuredOutputConfig;
import com.example.demo.dto.AssistRequest;
import com.example.demo.dto.AssistResponse;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Service
public class AssistService {

	private static final Logger log = LoggerFactory.getLogger(AssistService.class);

	private static final int MAX_TEXT_LENGTH = 20_000;
	private static final int MAX_CALLS_PER_MINUTE = 30;
	private static final String CURSOR = "<<CURSOR>>";

	private static final String SYSTEM_PROMPT = """
			You are an assistant built into a notes app. The user is typing a note, and you see the note \
			with %s marking where their cursor is. Whatever you return is inserted exactly at the cursor.

			Only respond to the text immediately before the cursor. Ignore anything earlier in the note, \
			and if what is before the cursor has already been answered, return an empty suggestion.

			There are two situations:

			1. The user explicitly asks you for something right before the cursor, for example \
			"please give me a recipe for beef curry", "write a short email to my landlord about the heater", \
			or "list 5 gift ideas for my dad". Answer what they asked for as one short paragraph in plain \
			text (no markdown). Hard limit: 100 words, which is about 5 sentences. If the full answer would \
			be longer, condense it (for a recipe: key ingredients and the main steps only) rather than go \
			over the limit.

			2. Otherwise, the user is just writing their note. Only add something when it is clearly useful:
			- the result of a calculation they just wrote ("12 * 7 =" -> "84")
			- a unit or currency conversion ("5 miles in km =" -> "8.05 km")
			- a short answer to a question they just wrote
			- a fact they are clearly about to write ("The capital of Australia is" -> "Canberra")
			Keep these to a few words, at most one sentence. Most of the time there is nothing to add, \
			so return an empty suggestion. Only suggest when you are confident it is correct and what the \
			user wants.

			In both cases, never repeat or rewrite text that is already in the note, and do not comment on, \
			summarize, or correct the note.
			""".formatted(CURSOR);

	// The shape Claude must reply in (enforced with structured output)
	public record ClaudeSuggestion(
			@JsonPropertyDescription("Text to insert at the cursor, or an empty string when there is nothing useful to add")
			String suggestion) {
	}

	private final AnthropicClient client;
	private final NoteService noteService;
	private final String model;
	private final Map<Long, Deque<Long>> recentCalls = new ConcurrentHashMap<>();

	public AssistService(AnthropicClient client, NoteService noteService, @Value("${anthropic.model}") String model) {
		this.client = client;
		this.noteService = noteService;
		this.model = model;
	}

	public AssistResponse assist(Long userId, Long noteId, AssistRequest request) {
		// Throws 404 if the note doesn't exist or belongs to someone else
		noteService.getNote(userId, noteId);

		String text = request.text() == null ? "" : request.text();
		if (text.length() > MAX_TEXT_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"note is too long for assist (max " + MAX_TEXT_LENGTH + " characters)");
		}
		int cursor = request.cursor() == null ? text.length() : request.cursor();
		if (cursor < 0 || cursor > text.length()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor is outside the text");
		}

		// Nothing typed before the cursor, so there's nothing to respond to; skip the Claude call
		if (text.substring(0, cursor).isBlank()) {
			return new AssistResponse(null);
		}

		checkRateLimit(userId);
		String suggestion = askClaude(text.substring(0, cursor) + CURSOR + text.substring(cursor));
		return new AssistResponse(suggestion.isBlank() ? null : suggestion);
	}

	private String askClaude(String noteWithCursor) {
		StructuredMessageCreateParams<ClaudeSuggestion> params = MessageCreateParams.builder()
				.model(model)
				.maxTokens(16000L)
				.system(SYSTEM_PROMPT)
				.addUserMessage("<note>\n" + noteWithCursor + "\n</note>")
				// If a safety classifier declines, the API retries on a fallback model instead of failing
				.addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
				.fallbacksDefault()
				// Low effort keeps responses fast; this task rarely needs deep reasoning
				.outputConfig(StructuredOutputConfig.<ClaudeSuggestion>builder()
						.format(ClaudeSuggestion.class, JsonSchemaLocalValidation.YES)
						.effort(BetaOutputConfig.Effort.LOW)
						.build())
				.build();

		try {
			var response = client.beta().messages().create(params);
			if (response.stopReason().map(BetaStopReason.REFUSAL::equals).orElse(false)) {
				return "";
			}
			return response.content().stream()
					.flatMap(block -> block.text().stream())
					.map(textBlock -> textBlock.text().suggestion())
					.filter(s -> s != null)
					.findFirst()
					.orElse("");
		}
		catch (RateLimitException e) {
			log.warn("Claude API rate limit hit", e);
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "assistant is busy, try again shortly");
		}
		catch (AnthropicException e) {
			log.error("Claude API call failed", e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "assistant is unavailable");
		}
	}

	// Simple in-memory limit: at most MAX_CALLS_PER_MINUTE Claude calls per user in any 60-second window
	private void checkRateLimit(Long userId) {
		long now = System.currentTimeMillis();
		Deque<Long> calls = recentCalls.computeIfAbsent(userId, id -> new ArrayDeque<>());
		synchronized (calls) {
			while (!calls.isEmpty() && now - calls.peekFirst() > 60_000) {
				calls.pollFirst();
			}
			if (calls.size() >= MAX_CALLS_PER_MINUTE) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many assist requests, slow down");
			}
			calls.addLast(now);
		}
	}

}
