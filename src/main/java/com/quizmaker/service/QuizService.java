package com.quizmaker.service;

import com.quizmaker.dto.QuizDto;
import com.quizmaker.model.Quiz;
import com.quizmaker.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;

    private static final JsonMapper JSON_MAPPER = new JsonMapper();
    private static final String QUIZ_NOT_FOUND_MESSAGE = "Quiz non trovato con id: %s";

    @Transactional(readOnly = true)
    public List<QuizDto.Response> findAll() {
        return quizRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDto.Response findById(UUID id) {
        return quizRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional
    public QuizDto.Response create(QuizDto.Request request) {
        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .emoji(request.getEmoji())
                .questions(request.getQuestions())
                .build();
        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz creato: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public QuizDto.Response update(UUID id, QuizDto.Request request) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
        quiz.setTitle(request.getTitle());
        quiz.setEmoji(request.getEmoji());
        quiz.setQuestions(request.getQuestions());
        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz aggiornato: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!quizRepository.existsById(id)) {
            throw new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id));
        }
        quizRepository.deleteById(id);
        log.info("Quiz eliminato: {}", id);
    }

    private QuizDto.Response toResponse(Quiz quiz) {
        return QuizDto.Response.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .emoji(quiz.getEmoji())
                .questions(quiz.getQuestions())
                .questionsCount(JSON_MAPPER.readValue(quiz.getQuestions(), List.class).size())
                .createdAt(quiz.getCreatedAt())
                .build();
    }

}
