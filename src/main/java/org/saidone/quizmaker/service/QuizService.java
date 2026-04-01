/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.mapper.QuestionMapper;
import org.saidone.quizmaker.mapper.QuizMapper;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
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
    public List<QuizDto.Response> findAllForAdmin(Teacher teacher) {
        return quizRepository.findAllByTeacherOrderByCreatedAtDesc(teacher)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizDto.Response> findPublishedForTeacher(Teacher teacher) {
        return quizRepository.findByTeacherAndPublishedTrueOrderByCreatedAtDesc(teacher)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDto.Response findByIdForTeacher(UUID id, Teacher teacher) {
        return quizRepository.findByIdAndTeacher(id, teacher)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional(readOnly = true)
    public QuizDto.Response findPublishedByIdForTeacher(UUID id, Teacher teacher) {
        return quizRepository.findByIdAndTeacherAndPublishedTrue(id, teacher)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional
    public QuizDto.Response create(QuizDto.Request request, Teacher teacher) {
        val quiz = Quiz.builder()
                .title(request.getTitle())
                .emoji(request.getEmoji())
                .questions(request.getQuestions().stream().map(questionMapper::toEntity).toList())
                .published(false)
                .teacher(teacher)
                .build();
        val saved = quizRepository.save(quiz);
        log.info("Quiz created: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public QuizDto.Response update(UUID id, QuizDto.Request request, Teacher teacher) {
        val quiz = quizRepository.findByIdAndTeacher(id, teacher)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
        quiz.setTitle(request.getTitle());
        quiz.setEmoji(request.getEmoji());
        quiz.setQuestions(request.getQuestions().stream().map(questionMapper::toEntity).toList());
        val saved = quizRepository.save(quiz);
        log.info("Quiz updated: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, Teacher teacher) {
        val quiz = quizRepository.findByIdAndTeacher(id, teacher)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, id)));
        quizSubmissionRepository.deleteAllByQuizIdAndQuizTeacher(id, teacher);
        quizRepository.delete(quiz);
        log.info("Quiz deleted: {}", id);
    }

    @Transactional
    public QuizDto.Response updatePublicationStatus(UUID id, boolean published, Teacher teacher) {
        val quiz = quizRepository.findByIdAndTeacher(id, teacher)
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
