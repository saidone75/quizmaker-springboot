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
import org.saidone.quizmaker.entity.Question;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSharingService {

    private static final String QUIZ_NOT_FOUND_MESSAGE = "Quiz non found for id: %s";

    private final QuizRepository quizRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher) && @teacherAuthorizationPolicy.canManageQuiz(#quizId, #actingTeacher)")
    public int shareQuizToTeachers(UUID quizId, List<UUID> destinationTeacherIds, Teacher actingTeacher) {
        val sourceQuiz = quizRepository.findByIdAndTeacher(quizId, actingTeacher)
                .orElseThrow(() -> new EntityNotFoundException(String.format(QUIZ_NOT_FOUND_MESSAGE, quizId)));

        val uniqueDestinationIds = destinationTeacherIds == null
                ? List.<UUID>of()
                : new ArrayList<>(new LinkedHashSet<>(destinationTeacherIds));

        val recipients = teacherRepository.findAllById(uniqueDestinationIds).stream()
                .filter(teacher -> !teacher.getId().equals(actingTeacher.getId()))
                .toList();

        int sharedCount = 0;
        int skippedCount = 0;
        for (val recipient : recipients) {
            if (isAlreadyShared(sourceQuiz, recipient)) {
                skippedCount++;
                log.info("Questa condivisione è già stata effettuata: sourceQuizId={}, recipientTeacherId={}, recipientUsername={}, by={}",
                        sourceQuiz.getId(), recipient.getId(), recipient.getUsername(), actingTeacher.getUsername());
                continue;
            }

            val clonedQuiz = Quiz.builder()
                    .title(sourceQuiz.getTitle())
                    .emoji(sourceQuiz.getEmoji())
                    .questions(cloneQuestions(sourceQuiz.getQuestions()))
                    .published(false)
                    .createdByUsername(sourceQuiz.getCreatedByUsername() != null ? sourceQuiz.getCreatedByUsername() : actingTeacher.getUsername())
                    .modifiedByUsername(actingTeacher.getUsername())
                    .modifiedAt(LocalDateTime.now())
                    .teacher(recipient)
                    .build();
            quizRepository.save(clonedQuiz);
            sharedCount++;
            log.info("Quiz condiviso: sourceQuizId={}, clonedQuizId={}, recipientTeacherId={}, recipientUsername={}, by={}",
                    sourceQuiz.getId(), clonedQuiz.getId(), recipient.getId(), recipient.getUsername(), actingTeacher.getUsername());
        }

        log.info("Condivisione quiz completata: sourceQuizId={}, requestedDestinations={}, recipientsResolved={}, sharedCount={}, skippedCount={}, by={}",
                sourceQuiz.getId(), uniqueDestinationIds.size(), recipients.size(), sharedCount, skippedCount, actingTeacher.getUsername());
        return sharedCount;
    }

    private boolean isAlreadyShared(Quiz sourceQuiz, Teacher recipient) {
        return quizRepository.existsByTitleAndTeacherAndCreatedByUsername(
                sourceQuiz.getTitle(),
                recipient,
                sourceQuiz.getCreatedByUsername()
        );
    }

    private List<Question> cloneQuestions(List<Question> sourceQuestions) {
        val clonedQuestions = new ArrayList<Question>();
        for (val sourceQuestion : sourceQuestions) {
            val clonedQuestion = new Question();
            clonedQuestion.setText(sourceQuestion.getText());
            clonedQuestion.setEmoji(sourceQuestion.getEmoji());
            clonedQuestion.setAnswer(sourceQuestion.getAnswer());
            clonedQuestion.setFeedback(sourceQuestion.getFeedback());
            clonedQuestion.setOptions(sourceQuestion.getOptions() == null ? List.of() : new ArrayList<>(sourceQuestion.getOptions()));
            clonedQuestions.add(clonedQuestion);
        }
        return clonedQuestions;
    }
}
