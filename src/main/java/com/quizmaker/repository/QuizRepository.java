package com.quizmaker.repository;

import com.quizmaker.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findAllByOrderByCreatedAtDesc();

    boolean existsByTitle(String title);
}
