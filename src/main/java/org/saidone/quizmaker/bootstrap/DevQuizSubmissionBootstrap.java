/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
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

package org.saidone.quizmaker.bootstrap;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.entity.QuizSubmission;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
@DependsOn({"devQuizBootstrap", "devStudentBootstrap"})
@Profile({"dev", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DevQuizSubmissionBootstrap {

    private static final String DEMO_QUIZ_TITLE = "Astronomia: il quiz delle stelle";
    private static final String DINO_QUIZ_TITLE = "Dinosauri: i giganti del Mesozoico";
    private static final int TARGET_SUBMISSIONS = 6;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    @PostConstruct
    public void init() {
        val teacher = teacherRepository.findByUsernameIgnoreCase(adminUsername).orElseThrow();
        val demoQuizzes = quizRepository.findAllByTeacherOrderByCreatedAtDesc(teacher).stream()
                .filter(quiz -> Set.of(DEMO_QUIZ_TITLE, DINO_QUIZ_TITLE).contains(quiz.getTitle()))
                .toList();

        if (demoQuizzes.isEmpty()) {
            log.info("Nessun quiz demo trovato, salto il bootstrap delle submission");
            return;
        }

        val selectedQuiz = demoQuizzes.get(ThreadLocalRandom.current().nextInt(demoQuizzes.size()));
        val students = studentRepository.findAllByTeacherOrderByFullNameAsc(teacher);
        if (students.isEmpty()) {
            log.info("Nessuno studente trovato, salto il bootstrap delle submission");
            return;
        }

        Collections.shuffle(students);
        var created = 0;

        for (val student : students) {
            if (created >= TARGET_SUBMISSIONS) {
                break;
            }

            if (quizSubmissionRepository.findByStudentIdAndQuizId(student.getId(), selectedQuiz.getId()).isPresent()) {
                continue;
            }

            val answers = randomAnswersForQuiz(selectedQuiz);
            val score = calculateScore(selectedQuiz, answers);

            quizSubmissionRepository.save(QuizSubmission.builder()
                    .student(student)
                    .quiz(selectedQuiz)
                    .answers(answers)
                    .score(score)
                    .totalQuestions(selectedQuiz.getQuestions().size())
                    .unlocked(false)
                    .build());

            created++;
        }

        log.info("Submission demo create: {} su quiz '{}'", created, selectedQuiz.getTitle());
    }

    private List<Integer> randomAnswersForQuiz(Quiz quiz) {
        return quiz.getQuestions().stream()
                .map(question -> ThreadLocalRandom.current().nextInt(question.getOptions().size()))
                .toList();
    }

    private int calculateScore(Quiz quiz, List<Integer> answers) {
        var score = 0;
        for (var i = 0; i < quiz.getQuestions().size(); i++) {
            if (quiz.getQuestions().get(i).getAnswer().equals(answers.get(i))) {
                score++;
            }
        }
        return score;
    }
}
