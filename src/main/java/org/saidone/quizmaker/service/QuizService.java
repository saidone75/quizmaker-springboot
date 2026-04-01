package org.saidone.quizmaker.service;

import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.mapper.QuestionMapper;
import org.saidone.quizmaker.mapper.QuizMapper;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
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
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;

    private static final String QUIZ_NOT_FOUND_MESSAGE = "Quiz non found for id: %s";

    @Transactional(readOnly = true)
    public List<QuizDto.Response> findAllForAdmin() {
        return quizRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizDto.Response> findPublished() {
        return quizRepository.findByPublishedTrueOrderByCreatedAtDesc()
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

    @Transactional(readOnly = true)
    public QuizDto.Response findPublishedById(UUID id) {
        return quizRepository.findByIdAndPublishedTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional
    public QuizDto.Response create(QuizDto.Request request) {
        val quiz = Quiz.builder()
                .title(request.getTitle())
                .emoji(request.getEmoji())
                .questions(request.getQuestions().stream().map(questionMapper::toEntity).toList())
                .published(false)
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
        val saved = quizRepository.save(quiz);
        log.info("Quiz updated: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!quizRepository.existsById(id)) {
            throw new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id));
        }
        quizSubmissionRepository.deleteAllByQuizId(id);
        quizRepository.deleteById(id);
        log.info("Quiz deleted: {}", id);
    }

    @Transactional
    public QuizDto.Response updatePublicationStatus(UUID id, boolean published) {
        val quiz = quizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
        quiz.setPublished(published);
        val saved = quizRepository.save(quiz);
        log.info("Quiz publication status updated: {} ({}) => {}", saved.getTitle(), saved.getId(), saved.getPublished());
        return toResponse(saved);
    }

    private QuizDto.Response toResponse(Quiz quiz) {
        return quizMapper.toResponse(quiz);
    }

}
