package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.GoogleAccount;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, Long> {

	@Query(value = "SELECT * FROM google_accounts WHERE google_sub = :googleSub", nativeQuery = true)
	Optional<GoogleAccount> findByGoogleSub(@Param("googleSub") String googleSub);

	// Insert on first sign-in, update on later ones. Google only sometimes sends a new refresh token,
	// so a missing one keeps the stored token instead of wiping it.
	@Transactional
	@Modifying
	@Query(value = """
			INSERT INTO google_accounts (user_id, google_sub, email, refresh_token, calendar_granted, updated_at)
			VALUES (:userId, :googleSub, :email, :refreshToken, :calendarGranted, now())
			ON CONFLICT (google_sub) DO UPDATE SET
				email = EXCLUDED.email,
				refresh_token = COALESCE(EXCLUDED.refresh_token, google_accounts.refresh_token),
				calendar_granted = EXCLUDED.calendar_granted,
				updated_at = now()
			""", nativeQuery = true)
	int upsert(@Param("userId") Long userId, @Param("googleSub") String googleSub, @Param("email") String email,
			@Param("refreshToken") String refreshToken, @Param("calendarGranted") boolean calendarGranted);

}
