package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.TokenResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.GoogleAccountRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GoogleOAuthClient.GoogleTokens;
import com.example.demo.service.GoogleOAuthClient.GoogleUserInfo;

// "Sign in with Google": logs in (or creates) a user and also asks for calendar access
@Service
public class GoogleAuthService {

	private static final String SCOPES = "openid email profile " + GoogleOAuthClient.CALENDAR_SCOPE;

	private final GoogleOAuthClient google;
	private final UserRepository userRepository;
	private final GoogleAccountRepository googleAccountRepository;
	private final TokenService tokenService;
	private final TextEncryptor tokenEncryptor;
	private final String redirectUri;
	private final SecureRandom random = new SecureRandom();

	public GoogleAuthService(GoogleOAuthClient google, UserRepository userRepository,
			GoogleAccountRepository googleAccountRepository, TokenService tokenService, TextEncryptor tokenEncryptor,
			@Value("${google.redirect-uri}") String redirectUri) {
		this.google = google;
		this.userRepository = userRepository;
		this.googleAccountRepository = googleAccountRepository;
		this.tokenService = tokenService;
		this.tokenEncryptor = tokenEncryptor;
		this.redirectUri = redirectUri;
	}

	// Random value that ties Google's redirect back to the browser that started the sign-in
	public String newState() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String authorizationUrl(String state) {
		return google.authorizationUrl(SCOPES, redirectUri, state);
	}

	public TokenResponse completeSignIn(String code, String state, String expectedState, String error) {
		google.requireConfigured();
		if (error != null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in was cancelled or denied: " + error);
		}
		if (state == null || expectedState == null || !MessageDigest.isEqual(
				state.getBytes(StandardCharsets.UTF_8), expectedState.getBytes(StandardCharsets.UTF_8))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"invalid or expired sign-in attempt, start again at /auth/google");
		}
		if (code == null || code.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing authorization code");
		}

		GoogleTokens tokens = google.exchangeCode(code, redirectUri);
		GoogleUserInfo info = google.fetchVerifiedUser(tokens.accessToken());

		// Users are matched by Google's account id, never by email, so nobody can claim
		// someone else's Google login by registering a username that equals their email
		User user = googleAccountRepository.findByGoogleSub(info.sub())
				.flatMap(account -> userRepository.findUserById(account.getUserId()))
				.orElseGet(() -> createUser(info));

		String encryptedRefreshToken = tokens.refreshToken() == null ? null
				: tokenEncryptor.encrypt(tokens.refreshToken());
		googleAccountRepository.upsert(user.getId(), info.sub(), info.email(), encryptedRefreshToken,
				tokens.grantedCalendar());

		return new TokenResponse(tokenService.createToken(user));
	}

	private User createUser(GoogleUserInfo info) {
		// Use the email as the username, unless an older account already took it
		String username = userRepository.findUserByUsername(info.email()).isEmpty() ? info.email()
				: "google-" + info.sub();
		return userRepository.insertUser(username);
	}

}
