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

package org.saidone.quizmaker.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultTeacherBootstrap implements CommandLineRunner {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final TeacherRepository teacherRepository;
    private final QuizRepository quizRepository;
    private final StudentRepository studentRepository;

    private String normalizeConfiguredPassword(String configuredPassword) {
        if (configuredPassword != null && configuredPassword.startsWith("{bcrypt}")) {
            return configuredPassword.substring(8);
        }
        return configuredPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        val defaultTeacher = teacherRepository.findByUsernameIgnoreCase(adminUsername)
                .orElseGet(() -> teacherRepository.save(Teacher.builder()
                        .username(adminUsername.trim().toLowerCase())
                        .password(normalizeConfiguredPassword(adminPassword))
                        .admin(true)
                        .aiEnabled(true)
                        .enabled(true)
                        .build()));

        var shouldSaveDefaultTeacher = false;
        if (!defaultTeacher.isAdmin()) {
            defaultTeacher.setAdmin(true);
            shouldSaveDefaultTeacher = true;
        }
        if (!defaultTeacher.isAiEnabled()) {
            defaultTeacher.setAiEnabled(true);
            shouldSaveDefaultTeacher = true;
        }
        if (!defaultTeacher.isEnabled()) {
            defaultTeacher.setEnabled(true);
            shouldSaveDefaultTeacher = true;
        }
        if (shouldSaveDefaultTeacher) {
            teacherRepository.save(defaultTeacher);
        }

        val quizzesWithoutTeacher = quizRepository.findAll().stream().filter(q -> q.getTeacher() == null).toList();
        quizzesWithoutTeacher.forEach(q -> q.setTeacher(defaultTeacher));
        if (!quizzesWithoutTeacher.isEmpty()) {
            quizRepository.saveAll(quizzesWithoutTeacher);
            log.info("Assegnati {} quiz senza tenant all'insegnante di default", quizzesWithoutTeacher.size());
        }

        val studentsWithoutTeacher = studentRepository.findAll().stream().filter(s -> s.getTeacher() == null).toList();
        studentsWithoutTeacher.forEach(s -> s.setTeacher(defaultTeacher));
        if (!studentsWithoutTeacher.isEmpty()) {
            studentRepository.saveAll(studentsWithoutTeacher);
            log.info("Assegnati {} studenti senza tenant all'insegnante di default", studentsWithoutTeacher.size());
        }
    }
}
