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

package org.saidone.quizmaker.policy;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TeacherAuthorizationPolicyTest {

    private final TeacherAuthorizationPolicy policy = new TeacherAuthorizationPolicy(mock(QuizRepository.class));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isTeacherShouldReturnTrueWhenAuthenticatedTeacherMatchesIgnoringCase() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "Alice",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        ));
        val actingTeacher = Teacher.builder().username("alice").build();

        assertThat(policy.isTeacher(actingTeacher)).isTrue();
    }

    @Test
    void isAdminShouldRequireBothAdminFlagAndAdminRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice",
                "n/a",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TEACHER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        ));
        val actingTeacher = Teacher.builder().username("alice").admin(true).build();

        assertThat(policy.isAdmin(actingTeacher)).isTrue();
    }

    @Test
    void isAdminShouldReturnFalseWhenTeacherFlagIsFalseEvenIfRoleIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice",
                "n/a",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TEACHER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        ));
        val actingTeacher = Teacher.builder().username("alice").admin(false).build();

        assertThat(policy.isAdmin(actingTeacher)).isFalse();
    }
}
