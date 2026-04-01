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

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StudentSessionService {

    public static final String STUDENT_ID_SESSION_KEY = "STUDENT_ID";

    private final StudentRepository studentRepository;

    public Optional<Student> getLoggedStudent(HttpSession session) {
        val studentId = session.getAttribute(STUDENT_ID_SESSION_KEY);
        if (studentId == null) {
            return Optional.empty();
        }
        return studentRepository.findById(UUID.fromString(String.valueOf(studentId)));
    }

    public Optional<Student> login(HttpSession session, String keyword) {
        return studentRepository.findByLoginKeywordIgnoreCase(keyword)
                .map(student -> {
                    session.setAttribute(STUDENT_ID_SESSION_KEY, student.getId().toString());
                    return student;
                });
    }

    public void logout(HttpSession session) {
        session.removeAttribute(STUDENT_ID_SESSION_KEY);
    }
}
