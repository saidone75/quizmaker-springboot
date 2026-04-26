/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.config.StudentAuthenticationToken;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentAuthenticationService {

    public Optional<Student> getCurrentStudentOptional() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof StudentAuthenticationToken studentAuthentication) {
            return Optional.ofNullable(studentAuthentication.getPrincipal());
        }

        return getStudentFromSession();
    }

    public Student getCurrentStudent() {
        return getCurrentStudentOptional()
                .orElseThrow(() -> new IllegalStateException("Studente non autenticato"));
    }

    private final StudentRepository studentRepository;

    private Optional<Student> getStudentFromSession() {
        val requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return Optional.empty();
        }

        HttpSession session = servletRequestAttributes.getRequest().getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object studentIdRaw = session.getAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY);
        if (studentIdRaw == null) {
            return Optional.empty();
        }

        try {
            val studentId = UUID.fromString(String.valueOf(studentIdRaw));
            return studentRepository.findById(studentId);
        } catch (IllegalArgumentException exception) {
            session.removeAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY);
            return Optional.empty();
        }
    }
}
