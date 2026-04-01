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
import lombok.val;
import org.saidone.quizmaker.dto.QuizSubmissionDto;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.entity.QuizSubmission;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizSubmissionService {

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    @Transactional(readOnly = true)
    public Set<UUID> findLockedQuizIdsForStudent(Student student) {
        return quizSubmissionRepository.findByStudent(student)
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getUnlocked()))
                .map(s -> s.getQuiz().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Transactional
    public QuizSubmissionDto.Response submit(UUID quizId, Student student, List<Integer> answers) {
        val quiz = quizRepository.findByIdAndPublishedTrue(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz non trovato: " + quizId));

        val existingSubmission = quizSubmissionRepository.findByStudentIdAndQuizId(student.getId(), quizId);
        if (existingSubmission.isPresent() && !Boolean.TRUE.equals(existingSubmission.get().getUnlocked())) {
            throw new IllegalStateException("Quiz già completato. La maestra deve sbloccarlo.");
        }

        int score = calculateScore(quiz, answers);

        val submission = existingSubmission.orElseGet(() -> QuizSubmission.builder()
                .student(student)
                .quiz(quiz)
                .build());

        submission.setAnswers(answers == null ? List.of() : answers);
        submission.setScore(score);
        submission.setTotalQuestions(quiz.getQuestions().size());
        submission.setUnlocked(false);
        quizSubmissionRepository.save(submission);

        return new QuizSubmissionDto.Response(quiz.getId(), score, quiz.getQuestions().size(), true);
    }

    @Transactional(readOnly = true)
    public List<ResultRow> findAllResults() {
        return quizSubmissionRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(s -> new ResultRow(
                        s.getStudent().getId(),
                        s.getStudent().getFullName(),
                        s.getQuiz().getId(),
                        s.getQuiz().getTitle(),
                        s.getScore(),
                        s.getTotalQuestions(),
                        s.getSubmittedAt(),
                        Boolean.TRUE.equals(s.getUnlocked())
                ))
                .toList();
    }

    @Transactional
    public void unlockQuizForStudent(UUID studentId, UUID quizId) {
        val submission = quizSubmissionRepository.findByStudentIdAndQuizId(studentId, quizId)
                .orElseThrow(() -> new EntityNotFoundException("Consegna non trovata per studente/quiz"));
        submission.setUnlocked(true);
        quizSubmissionRepository.save(submission);
    }

    @Transactional
    public int unlockAllForQuiz(UUID quizId) {
        val submissions = quizSubmissionRepository.findByQuizIdAndUnlockedFalse(quizId);
        submissions.forEach(submission -> submission.setUnlocked(true));
        quizSubmissionRepository.saveAll(submissions);
        return submissions.size();
    }

    public record ResultRow(
            UUID studentId,
            String studentName,
            UUID quizId,
            String quizTitle,
            Integer score,
            Integer totalQuestions,
            LocalDateTime submittedAt,
            boolean unlocked
    ) {
    }

    private int calculateScore(Quiz quiz, List<Integer> answers) {
        if (answers == null) {
            return 0;
        }
        int score = 0;
        int max = Math.min(answers.size(), quiz.getQuestions().size());
        for (int i = 0; i < max; i++) {
            val given = answers.get(i);
            val expected = quiz.getQuestions().get(i).getAnswer();
            if (Objects.equals(given, expected)) {
                score++;
            }
        }
        return score;
    }
}
