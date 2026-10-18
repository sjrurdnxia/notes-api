package com.example.demo.controller;

import java.net.URI;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.TokenResponse;
import com.example.demo.service.GoogleAuthService;

// Open /auth/google in a browser (not Swagger): it redirects to Google's sign-in page
@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {

	private static final String STATE_COOKIE = "google_oauth_state";

	private final GoogleAuthService googleAuthService;

	public GoogleAuthController(GoogleAuthService googleAuthService) {
		this.googleAuthService = googleAuthService;
	}

	@GetMapping
	public ResponseEntity<Void> start(HttpServletRequest request) {
		String state = googleAuthService.newState();
		String url = googleAuthService.authorizationUrl(state);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(url))
				.header(HttpHeaders.SET_COOKIE, stateCookie(state, Duration.ofMinutes(10), request).toString())
				.build();
	}

	// Google redirects the browser here after the user signs in (or cancels)
	@GetMapping("/callback")
	public ResponseEntity<TokenResponse> callback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state, @RequestParam(required = false) String error,
			@CookieValue(name = STATE_COOKIE, required = false) String expectedState, HttpServletRequest request) {
		TokenResponse token = googleAuthService.completeSignIn(code, state, expectedState, error);
		return ResponseEntity.ok()
				// The state is single-use, so clear the cookie
				.header(HttpHeaders.SET_COOKIE, stateCookie("", Duration.ZERO, request).toString())
				.body(token);
	}

	private static ResponseCookie stateCookie(String value, Duration maxAge, HttpServletRequest request) {
		return ResponseCookie.from(STATE_COOKIE, value)
				.httpOnly(true)
				.secure(request.isSecure())
				// Lax still sends the cookie on Google's top-level redirect back to the callback
				.sameSite("Lax")
				.path("/auth/google")
				.maxAge(maxAge)
				.build();
	}

}
