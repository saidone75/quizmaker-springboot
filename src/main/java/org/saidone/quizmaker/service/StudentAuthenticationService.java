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

import lombok.val;
import org.saidone.quizmaker.config.StudentAuthenticationToken;
import org.saidone.quizmaker.entity.Student;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentAuthenticationService {

    public Optional<Student> getCurrentStudentOptional() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof StudentAuthenticationToken studentAuthentication)) {
            return Optional.empty();
        }

        return Optional.ofNullable(studentAuthentication.getPrincipal());
    }

    public Student getCurrentStudent() {
        return getCurrentStudentOptional()
                .orElseThrow(() -> new IllegalStateException("Studente non autenticato"));
    }
}
