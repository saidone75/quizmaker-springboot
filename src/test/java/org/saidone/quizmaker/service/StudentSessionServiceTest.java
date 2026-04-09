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

package org.saidone.quizmaker.service;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.saidone.quizmaker.config.StudentAuthenticationToken;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentSessionServiceTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final StudentSessionService service = new StudentSessionService(studentRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldStoreStudentIdWithoutReplacingExistingAuthentication() {
        val existingAuth = new UsernamePasswordAuthenticationToken("teacher", "secret");
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        val keyword = "abcde";
        val student = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Mario Rossi")
                .loginKeyword("ABCDE")
                .build();

        when(studentRepository.findByLoginKeywordIgnoreCase(keyword)).thenReturn(Optional.of(student));

        val session = new MockHttpSession();
        val result = service.login(session, keyword);

        assertThat(result).contains(student);
        assertThat(session.getAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY)).isEqualTo(student.getId().toString());
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isEqualTo(existingAuth);
    }

    @Test
    void logoutShouldRemoveOnlyStudentSessionAndPreserveTeacherAuthentication() {
        val session = new MockHttpSession();
        session.setAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY, UUID.randomUUID().toString());
        val teacherAuth = new UsernamePasswordAuthenticationToken("teacher", "n/a");
        SecurityContextHolder.getContext().setAuthentication(teacherAuth);

        service.logout(session);

        assertThat(session.getAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(teacherAuth);
    }

    @Test
    void logoutShouldClearStudentAuthentication() {
        val session = new MockHttpSession();
        session.setAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY, UUID.randomUUID().toString());
        val student = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Mario Rossi")
                .loginKeyword("ABCDE")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new StudentAuthenticationToken(student));

        service.logout(session);

        assertThat(session.getAttribute(StudentSessionService.STUDENT_ID_SESSION_KEY)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
