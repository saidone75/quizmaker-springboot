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

import lombok.RequiredArgsConstructor;
import lombok.val;
import net.datafaker.Faker;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherAdministrationService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Faker FAKER = new Faker(Locale.ITALIAN);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public List<Teacher> findAllTeachers(Teacher actingTeacher) {
        return teacherRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public void updateTeacherAdminFlag(UUID targetTeacherId, boolean admin, Teacher actingTeacher) {
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId)) {
            throw new IllegalArgumentException("Non puoi modificare il tuo ruolo amministratore");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setAdmin(admin);
        teacherRepository.save(targetTeacher);
    }

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public void updateTeacherAiFlag(UUID targetTeacherId, boolean aiEnabled, Teacher actingTeacher) {
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setAiEnabled(aiEnabled);
        teacherRepository.save(targetTeacher);
    }

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public void updateTeacherEnabledFlag(UUID targetTeacherId, boolean enabled, Teacher actingTeacher) {
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId) && !enabled) {
            throw new IllegalArgumentException("Non puoi disabilitare il tuo account");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setEnabled(enabled);
        teacherRepository.save(targetTeacher);
    }

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public String resetTeacherPassword(UUID targetTeacherId, Teacher actingTeacher) {
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));

        String temporaryPassword;
        do {
            temporaryPassword = String.format("%s%02d", FAKER.animal().name(), RANDOM.nextInt(100));
        } while (temporaryPassword.length() < 6 || temporaryPassword.length() > 16 || temporaryPassword.contains(" "));

        targetTeacher.setPassword(passwordEncoder.encode(temporaryPassword));
        teacherRepository.save(targetTeacher);
        return temporaryPassword;
    }
}
