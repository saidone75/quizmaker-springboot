package com.quizmaker.service;

import com.quizmaker.dto.QuizDto;
import com.quizmaker.entity.Quiz;
import com.quizmaker.mapper.QuestionMapper;
import com.quizmaker.mapper.QuizMapper;
import com.quizmaker.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;

    private static final String QUIZ_NOT_FOUND_MESSAGE = "Quiz non found for id: %s";

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
        val quiz = Quiz.builder()
                .title(request.getTitle())
                .emoji(request.getEmoji())
                .questions(request.getQuestions().stream().map(questionMapper::toEntity).toList())
                .build();
        val saved = quizRepository.save(quiz);
        log.info("Quiz created: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public QuizDto.Response update(UUID id, QuizDto.Request request) {
        val quiz = quizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
        quiz.setTitle(request.getTitle());
        quiz.setEmoji(request.getEmoji());
        quiz.setQuestions(request.getQuestions().stream().map(questionMapper::toEntity).toList());
        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz updated: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!quizRepository.existsById(id)) {
            throw new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id));
        }
        quizRepository.deleteById(id);
        log.info("Quiz deleted: {}", id);
    }

    private QuizDto.Response toResponse(Quiz quiz) {
        return quizMapper.toResponse(quiz);
    }

}
