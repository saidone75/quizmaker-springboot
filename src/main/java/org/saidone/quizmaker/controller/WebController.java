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

package org.saidone.quizmaker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.service.QuizSubmissionService;
import org.saidone.quizmaker.service.StudentService;
import org.saidone.quizmaker.service.StudentSessionService;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final QuizService quizService;
    private final QuizSubmissionService quizSubmissionService;
    private final StudentSessionService studentSessionService;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;
    private final BuildProperties buildProperties;

    @GetMapping("/")
    public String studentPage(HttpSession session, Model model) {
        val maybeStudent = studentSessionService.getLoggedStudent(session);
        if (maybeStudent.isEmpty()) {
            return "student-login";
        }

        val quizzes = quizService.findPublished();
        val lockedQuizIds = quizSubmissionService.findLockedQuizIdsForStudent(maybeStudent.get());

        model.addAttribute("studentName", maybeStudent.get().getFullName());
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

    @PostMapping("/student/login")
    public String studentLogin(@RequestParam("keyword") String keyword, HttpSession session, Model model) {
        if (keyword == null || keyword.trim().length() != 4) {
            model.addAttribute("loginError", "La parola chiave deve avere 4 caratteri.");
            return "student-login";
        }

        return studentSessionService.login(session, keyword.trim())
                .map(s -> "redirect:/")
                .orElseGet(() -> {
                    model.addAttribute("loginError", "Parola chiave non valida.");
                    return "student-login";
                });
    }

    @PostMapping("/student/logout")
    public String studentLogout(HttpSession session) {
        studentSessionService.logout(session);
        return "redirect:/";
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("quizzes", quizService.findAllForAdmin());
        return "admin/dashboard";
    }

    @GetMapping("/admin/students")
    public String adminStudents(Model model) {
        model.addAttribute("students", studentService.findAll());
        return "admin/students";
    }

    @GetMapping("/admin/logs")
    public String adminLogs() {
        return "admin/logs";
    }

    @GetMapping("/admin/results")
    public String adminResults(Model model) {
        val results = quizSubmissionService.findAllResults();

        val groupedResults = results.stream()
                .collect(Collectors.groupingBy(
                        QuizSubmissionService.ResultRow::quizId,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new QuizResultGroup(
                        entry.getKey(),
                        entry.getValue().get(0).quizTitle(),
                        entry.getValue()))
                .toList();

        model.addAttribute("groupedResults", groupedResults);
        return "admin/results";
    }

    @GetMapping("/admin/quiz/new")
    public String newQuiz() {
        return "admin/quiz-editor";
    }

    @GetMapping("/admin/quiz/{id}/edit")
    public String editQuiz(@PathVariable UUID id, Model model) {
        model.addAttribute("quiz", quizService.findById(id));
        return "admin/quiz-editor";
    }

    @GetMapping({"/about", "/admin/about"})
    public String aboutPage(Model model) {
        val runtime = Runtime.getRuntime();
        model.addAttribute("appVersion", getAppVersion());
        model.addAttribute("buildTime", getBuildTime());
        model.addAttribute("springBootVersion", SpringBootVersion.getVersion());
        model.addAttribute("springFrameworkVersion", SpringVersion.getVersion());
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("javaVendor", System.getProperty("java.vendor"));
        model.addAttribute("jvmName", System.getProperty("java.vm.name"));
        model.addAttribute("jvmVersion", System.getProperty("java.vm.version"));
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("osVersion", System.getProperty("os.version"));
        model.addAttribute("osArch", System.getProperty("os.arch"));
        model.addAttribute("availableProcessors", runtime.availableProcessors());
        model.addAttribute("heapMaxMb", runtime.maxMemory() / (1024 * 1024));
        model.addAttribute("heapTotalMb", runtime.totalMemory() / (1024 * 1024));
        model.addAttribute("heapFreeMb", runtime.freeMemory() / (1024 * 1024));
        return "about";
    }

    private String getAppVersion() {
        return buildProperties != null ? buildProperties.getVersion() : Strings.EMPTY;
    }

    private String getBuildTime() {
        if (buildProperties == null || buildProperties.getTime() == null) {
            return Strings.EMPTY;
        }

        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(buildProperties.getTime());
    }

    private record QuizResultGroup(UUID quizId, String quizTitle, List<QuizSubmissionService.ResultRow> results) {
    }
}
