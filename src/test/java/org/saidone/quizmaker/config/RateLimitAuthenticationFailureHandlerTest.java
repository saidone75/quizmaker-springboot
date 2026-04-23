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

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.saidone.quizmaker.service.BruteForceProtectionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAuthenticationFailureHandlerTest {

    @Mock
    private BruteForceProtectionService bruteForceProtectionService;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private RateLimitAuthenticationFailureHandler handler;

    @Test
    void shouldRedirectToPendingAccountWhenTeacherIsDisabledPendingApproval() throws Exception {
        val request = new MockHttpServletRequest();
        request.setParameter("username", "pendingTeacher");
        request.setRemoteAddr("127.0.0.1");
        val response = new MockHttpServletResponse();
        val pendingTeacher = Teacher.builder().username("pendingteacher").approvalPending(true).enabled(false).build();

        doNothing().when(bruteForceProtectionService).recordLoginFailure(anyString());
        when(bruteForceProtectionService.isLoginBlocked(anyString())).thenReturn(false);
        when(teacherRepository.findByUsernameIgnoreCase("pendingTeacher")).thenReturn(Optional.of(pendingTeacher));

        handler.onAuthenticationFailure(request, response, new DisabledException("disabled"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/teacher/login?pendingAccount=true");
        verify(teacherRepository).findByUsernameIgnoreCase("pendingTeacher");
    }

    @Test
    void shouldFallbackToGenericErrorForBadCredentials() throws Exception {
        val request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setParameter("username", "teacher");
        val response = new MockHttpServletResponse();

        doNothing().when(bruteForceProtectionService).recordLoginFailure(anyString());
        when(bruteForceProtectionService.isLoginBlocked(anyString())).thenReturn(false);

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/teacher/login?error=true");
        verify(teacherRepository, never()).findByUsernameIgnoreCase(anyString());
    }
}
