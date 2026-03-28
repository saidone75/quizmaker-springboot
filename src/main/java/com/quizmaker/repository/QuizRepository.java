package com.quizmaker.repository;

import com.quizmaker.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findAllByOrderByCreatedAtDesc();

    List<Quiz> findByPublishedTrueOrderByCreatedAtDesc();

    Optional<Quiz> findByIdAndPublishedTrue(UUID id);

    boolean existsByTitle(String title);
}
