package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Note;

// Every query filters on user_id so users can only ever touch their own notes
public interface NoteRepository extends JpaRepository<Note, Long> {

	@Transactional
	@Query(value = """
			INSERT INTO notes (user_id, title, content, created_at, updated_at)
			VALUES (:userId, :title, :content, now(), now())
			RETURNING *
			""", nativeQuery = true)
	Note insertNote(@Param("userId") Long userId, @Param("title") String title, @Param("content") String content);

	@Query(value = "SELECT * FROM notes WHERE user_id = :userId ORDER BY updated_at DESC", nativeQuery = true)
	List<Note> findNotesByUser(@Param("userId") Long userId);

	@Query(value = "SELECT * FROM notes WHERE id = :id AND user_id = :userId", nativeQuery = true)
	Optional<Note> findNote(@Param("id") Long id, @Param("userId") Long userId);

	@Transactional
	@Query(value = """
			UPDATE notes SET title = :title, content = :content, updated_at = now()
			WHERE id = :id AND user_id = :userId
			RETURNING *
			""", nativeQuery = true)
	Optional<Note> updateNote(@Param("id") Long id, @Param("userId") Long userId,
			@Param("title") String title, @Param("content") String content);

	// No RETURNING here, so @Modifying is needed; it returns the number of rows deleted
	@Transactional
	@Modifying
	@Query(value = "DELETE FROM notes WHERE id = :id AND user_id = :userId", nativeQuery = true)
	int deleteNote(@Param("id") Long id, @Param("userId") Long userId);

}
