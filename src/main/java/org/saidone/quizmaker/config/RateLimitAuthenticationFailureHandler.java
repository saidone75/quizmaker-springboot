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

package org.saidone.quizmaker.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.jspecify.annotations.NonNull;
import org.saidone.quizmaker.service.BruteForceProtectionService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final BruteForceProtectionService bruteForceProtectionService;
    private final TeacherRepository teacherRepository;

    @Override
    public void onAuthenticationFailure(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull AuthenticationException exception) throws IOException {
        val key = RequestFingerprint.loginKey(request);
        bruteForceProtectionService.recordLoginFailure(key);

        if (bruteForceProtectionService.isLoginBlocked(key)) {
            response.sendRedirect("/teacher/login?blocked=true");
            return;
        }

        if (exception instanceof DisabledException) {
            val username = request.getParameter("username");
            if (username != null && teacherRepository.findByUsernameIgnoreCase(username).map(teacher -> teacher.isApprovalPending()).orElse(false)) {
                response.sendRedirect("/teacher/login?pendingAccount=true");
                return;
            }
        }

        response.sendRedirect("/teacher/login?error=true");
    }
}
