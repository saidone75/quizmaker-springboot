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
import org.saidone.quizmaker.service.TeacherAuthService;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final QuizService quizService;
    private final QuizSubmissionService quizSubmissionService;
    private final StudentSessionService studentSessionService;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;
    private final BuildProperties buildProperties;
    private final TeacherAuthService teacherAuthService;

    @GetMapping("/")
    public String studentPage(HttpSession session, Model model) {
        val maybeStudent = studentSessionService.getLoggedStudent(session);
        if (maybeStudent.isEmpty()) {
            return "student-login";
        }

        val quizzes = quizService.findPublishedForTeacher(maybeStudent.get().getTeacher());
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
        if (keyword == null || keyword.trim().length() != 5) {
            model.addAttribute("loginError", "La parola chiave deve avere 5 caratteri.");
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

    @GetMapping("/teacher/login")
    public String loginPage() {
        return "admin/login";
    }


    @GetMapping("/teacher/register")
    public String registerPage() {
        return "admin/register";
    }

    @PostMapping("/teacher/register")
    public String registerTeacher(@RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("registerError", "Le password non coincidono.");
            model.addAttribute("username", username);
            return "admin/register";
        }

        try {
            teacherAuthService.register(username, password);
            return "redirect:/teacher/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registerError", ex.getMessage());
            model.addAttribute("username", username);
            return "admin/register";
        }
    }

    @GetMapping("/teacher")
    public String adminDashboard(Model model) {
        val currentTeacher = teacherAuthService.getCurrentTeacher();
        model.addAttribute("quizzes", quizService.findAllForAdmin(currentTeacher));
        model.addAttribute("isAdmin", currentTeacher.isAdmin());
        if (currentTeacher.isAdmin()) {
            val shareTeachers = teacherAuthService.findAllTeachers().stream()
                    .filter(teacher -> !teacher.getId().equals(currentTeacher.getId()))
                    .map(teacher -> new ShareTeacherOption(teacher.getId(), teacher.getUsername()))
                    .toList();
            model.addAttribute("shareTeachers", shareTeachers);
        } else {
            model.addAttribute("shareTeachers", List.of());
        }
        return "admin/dashboard";
    }

    @GetMapping("/teacher/students")
    public String adminStudents(Model model) {
        model.addAttribute("students", studentService.findAll(teacherAuthService.getCurrentTeacher()));
        return "admin/students";
    }

    @GetMapping("/teacher/logs")
    public String adminLogs() {
        return "admin/logs";
    }

    @GetMapping("/teacher/results")
    public String adminResults(Model model) {
        val results = quizSubmissionService.findAllResults(teacherAuthService.getCurrentTeacher());

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

    @GetMapping("/teacher/profile")
    public String teacherProfilePage() {
        return "admin/profile";
    }

    @PostMapping("/teacher/profile/password")
    public String changeTeacherPassword(@RequestParam("currentPassword") String currentPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("profileError", "Le nuove password non coincidono.");
            return "admin/profile";
        }

        try {
            teacherAuthService.changePassword(teacherAuthService.getCurrentTeacher(), currentPassword, newPassword);
            model.addAttribute("profileSuccess", "Password aggiornata con successo.");
            return "admin/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("profileError", ex.getMessage());
            return "admin/profile";
        }
    }

    @GetMapping("/teacher/quiz/new")
    public String newQuiz(Model model) {
        model.addAttribute("aiEnabled", teacherAuthService.getCurrentTeacher().isAiEnabled());
        return "admin/quiz-editor";
    }

    @GetMapping("/teacher/quiz/{id}/edit")
    public String editQuiz(@PathVariable UUID id, Model model) {
        model.addAttribute("aiEnabled", teacherAuthService.getCurrentTeacher().isAiEnabled());
        model.addAttribute("quiz", quizService.findByIdForTeacher(id, teacherAuthService.getCurrentTeacher()));
        return "admin/quiz-editor";
    }

    @GetMapping("/teacher/system")
    public String systemPage() {
        ensureAdmin();
        return "admin/system";
    }

    @GetMapping("/teacher/system/teachers")
    public String teacherManagementPage(Model model) {
        ensureAdmin();
        val currentTeacher = teacherAuthService.getCurrentTeacher();
        val teachers = teacherAuthService.findAllTeachers();
        model.addAttribute("teachers", teachers);
        model.addAttribute("currentTeacherId", currentTeacher.getId());
        return "admin/system-teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/admin")
    public String updateTeacherAdminFlag(@PathVariable UUID id,
                                         @RequestParam("admin") boolean admin) {
        ensureAdmin();
        teacherAuthService.updateTeacherAdminFlag(id, admin, teacherAuthService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }



    @PostMapping("/teacher/system/teachers/{id}/ai")
    public String updateTeacherAiFlag(@PathVariable UUID id,
                                      @RequestParam("aiEnabled") boolean aiEnabled) {
        ensureAdmin();
        teacherAuthService.updateTeacherAiFlag(id, aiEnabled, teacherAuthService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/enabled")
    public String updateTeacherEnabledFlag(@PathVariable UUID id,
                                           @RequestParam("enabled") boolean enabled) {
        ensureAdmin();
        teacherAuthService.updateTeacherEnabledFlag(id, enabled, teacherAuthService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/reset-password")
    public String resetTeacherPassword(@PathVariable UUID id,
                                       RedirectAttributes redirectAttributes) {
        ensureAdmin();
        val temporaryPassword = teacherAuthService.resetTeacherPassword(id, teacherAuthService.getCurrentTeacher());
        redirectAttributes.addFlashAttribute("teacherResetSuccess", String.format("Password resettata correttamente a %s", temporaryPassword));
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/delete")
    public String deleteTeacher(@PathVariable UUID id) {
        ensureAdmin();
        teacherAuthService.deleteTeacherCompletely(id, teacherAuthService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @GetMapping({"/about", "/teacher/about", "/teacher/system/about"})
    public String aboutPage(Model model) {
        ensureAdmin();
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

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode() == FORBIDDEN) {
            return "redirect:/teacher";
        }
        throw ex;
    }

    private void ensureAdmin() {
        if (!teacherAuthService.getCurrentTeacher().isAdmin()) {
            throw new ResponseStatusException(FORBIDDEN);
        }
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

    private record ShareTeacherOption(UUID id, String username) {
    }
}
