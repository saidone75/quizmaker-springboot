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

package org.saidone.quizmaker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.config.RequestFingerprint;
import org.saidone.quizmaker.service.BruteForceProtectionService;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.service.QuizSubmissionService;
import org.saidone.quizmaker.service.StudentSessionService;
import org.saidone.quizmaker.service.StudentAuthenticationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class StudentWebController {

    private final QuizService quizService;
    private final QuizSubmissionService quizSubmissionService;
    private final StudentSessionService studentSessionService;
    private final StudentAuthenticationService studentAuthenticationService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String studentPage(Model model) {
        val maybeStudent = studentAuthenticationService.getCurrentStudentOptional();
        if (maybeStudent.isEmpty()) {
            return "student-login";
        }

        val quizzes = quizService.findPublishedForTeacher(maybeStudent.get().getTeacher());
        val lockedQuizIds = quizSubmissionService.findLockedQuizIdsForStudent(maybeStudent.get());

        model.addAttribute("studentName", maybeStudent.get().getFullName());
        model.addAttribute("studentId", maybeStudent.get().getId());
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("lockedQuizIds", lockedQuizIds);
        try {
            model.addAttribute("quizzesJson", objectMapper.writeValueAsString(quizzes));
            model.addAttribute("lockedQuizIdsJson", objectMapper.writeValueAsString(lockedQuizIds));
        } catch (JsonProcessingException e) {
            model.addAttribute("quizzesJson", "[]");
            model.addAttribute("lockedQuizIdsJson", "[]");
        }
        return "student";
    }

    @GetMapping("/student/login")
    public String studentLoginPage() {
        return "redirect:/";
    }

    @PostMapping("/student/login")
    public String studentLogin(@RequestParam("keyword") String keyword,
                               HttpServletRequest request,
                               HttpSession session,
                               Model model) {
        if (keyword == null || keyword.trim().length() != 5) {
            model.addAttribute("loginError", "La parola chiave deve avere 5 caratteri.");
            return "student-login";
        }

        val normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        val clientIp = RequestFingerprint.clientIp(request);
        if (bruteForceProtectionService.isStudentLoginBlocked(clientIp, normalizedKeyword)) {
            model.addAttribute("loginError", "Troppi tentativi di accesso. Riprova tra qualche minuto.");
            return "student-login";
        }

        return studentSessionService.login(session, normalizedKeyword)
                .map(s -> {
                    bruteForceProtectionService.clearStudentLoginFailures(clientIp, normalizedKeyword);
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    bruteForceProtectionService.recordStudentLoginFailureByIp(clientIp);
                    bruteForceProtectionService.recordStudentLoginFailureByKeyword(normalizedKeyword);
                    model.addAttribute("loginError", "Parola chiave non valida.");
                    return "student-login";
                });
    }

    @PostMapping("/student/logout")
    public String studentLogout(HttpSession session) {
        studentSessionService.logout(session);
        return "redirect:/";
    }
}
