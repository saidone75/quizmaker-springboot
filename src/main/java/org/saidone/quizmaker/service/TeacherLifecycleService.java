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
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherLifecycleService {

    private final TeacherRepository teacherRepository;
    private final QuizRepository quizRepository;
    private final StudentRepository studentRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    @Transactional
    @PreAuthorize("@teacherAuthorizationPolicy.isAdmin(#actingTeacher)")
    public void deleteTeacherCompletely(UUID targetTeacherId, Teacher actingTeacher) {
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId)) {
            throw new IllegalArgumentException("Non puoi eliminare l'utente attualmente loggato");
        }
        if (teacherRepository.countByIdNot(targetTeacherId) == 0) {
            throw new IllegalArgumentException("Impossibile eliminare l'ultimo insegnante");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));

        quizSubmissionRepository.deleteAllByStudentTeacherOrQuizTeacher(targetTeacher, targetTeacher);
        quizRepository.deleteAllByTeacher(targetTeacher);
        studentRepository.deleteAllByTeacher(targetTeacher);
        teacherRepository.delete(targetTeacher);
    }
}
