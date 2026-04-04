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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DevTeacherBootstrap {

    private static final String DEV_TEACHER_USERNAME = "pincopanco";
    private static final String DEV_TEACHER_PASSWORD = "pancopinco";

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        val existingTeacher = teacherRepository.findByUsernameIgnoreCase(DEV_TEACHER_USERNAME).orElse(null);
        if (existingTeacher == null) {
            teacherRepository.save(Teacher.builder()
                    .username(DEV_TEACHER_USERNAME)
                    .password(passwordEncoder.encode(DEV_TEACHER_PASSWORD))
                    .admin(false)
                    .aiEnabled(false)
                    .enabled(true)
                    .build());
            log.info("Creato insegnante demo '{}' con AI disabilitata", DEV_TEACHER_USERNAME);
            return;
        }

        var shouldSave = false;
        if (existingTeacher.isAiEnabled()) {
            existingTeacher.setAiEnabled(false);
            shouldSave = true;
        }
        if (existingTeacher.isAdmin()) {
            existingTeacher.setAdmin(false);
            shouldSave = true;
        }
        if (!existingTeacher.isEnabled()) {
            existingTeacher.setEnabled(true);
            shouldSave = true;
        }

        if (shouldSave) {
            teacherRepository.save(existingTeacher);
            log.info("Allineato insegnante demo '{}' con impostazioni demo", DEV_TEACHER_USERNAME);
        }
    }

}
