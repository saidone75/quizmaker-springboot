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

package org.saidone.quizmaker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.service.StudentSessionService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentSessionAuthenticationFilter extends OncePerRequestFilter {

    private final StudentRepository studentRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        val context = SecurityContextHolder.getContext();
        val authentication = context.getAuthentication();

        boolean missingOrAnonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;
        if (missingOrAnonymous) {
            val session = request.getSession(false);
            if (session != null) {
                val sessionStudentId = session.getAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY);
                if (sessionStudentId != null) {
                    try {
                        val studentId = UUID.fromString(String.valueOf(sessionStudentId));
                        studentRepository.findById(studentId).ifPresent(student -> {
                            val studentAuth = new StudentAuthenticationToken(student);
                            studentAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            context.setAuthentication(studentAuth);
                        });
                    } catch (IllegalArgumentException ignored) {
                        session.removeAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
