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

import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("teacherAuthorizationPolicy")
public class TeacherAuthorizationPolicy {

    private static final String ROLE_TEACHER = "ROLE_TEACHER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final QuizRepository quizRepository;

    public TeacherAuthorizationPolicy(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public boolean isTeacher(Teacher actingTeacher) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || actingTeacher == null
                || authentication.getName() == null) {
            return false;
        }

        return hasAuthority(authentication, ROLE_TEACHER)
                && authentication.getName().equalsIgnoreCase(actingTeacher.getUsername());
    }

    public boolean isAdmin(Teacher actingTeacher) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !actingTeacher.isAdmin() || !isTeacher(actingTeacher)) {
            return false;
        }

        return hasAuthority(authentication, ROLE_ADMIN);
    }

    public boolean canManageQuiz(UUID quizId, Teacher actingTeacher) {
        if (quizId == null || !isTeacher(actingTeacher)) {
            return false;
        }

        return quizRepository.findByIdAndTeacher(quizId, actingTeacher).isPresent();
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
