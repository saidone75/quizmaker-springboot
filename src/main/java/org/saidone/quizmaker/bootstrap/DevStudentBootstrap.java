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
import net.datafaker.Faker;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.saidone.quizmaker.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
@Profile({"dev", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DevStudentBootstrap implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    private static final Faker FAKER = new Faker(Locale.ITALIAN);

    @Override
    public void run(String... args) {
        val teacher = teacherRepository.findAll().stream().findFirst().orElseThrow();

        for (var i = 0; i < 6; i++) {
            val student = Student.builder()
                    .id(UUID.randomUUID())
                    .fullName(String.format("%s %s", FAKER.name().firstName(), FAKER.name().lastName()))
                    .loginKeyword(StudentService.randomAlphanumeric(5))
                    .teacher(teacher)
                    .build();
            studentRepository.save(student);
        }

        log.info("Studenti demo creati!");
    }

}
