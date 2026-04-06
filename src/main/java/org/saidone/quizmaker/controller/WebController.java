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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.saidone.quizmaker.config.RequestFingerprint;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.service.BruteForceProtectionService;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.service.QuizSubmissionService;
import org.saidone.quizmaker.service.StudentService;
import org.saidone.quizmaker.service.StudentSessionService;
import org.saidone.quizmaker.service.TeacherAdministrationService;
import org.saidone.quizmaker.service.TeacherAuthenticationService;
import org.saidone.quizmaker.service.TeacherLifecycleService;
import org.saidone.quizmaker.service.TurnstileCaptchaService;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private final TeacherAuthenticationService teacherAuthenticationService;
    private final TeacherAdministrationService teacherAdministrationService;
    private final TeacherLifecycleService teacherLifecycleService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final TurnstileCaptchaService turnstileCaptchaService;

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

    @GetMapping("/teacher/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/teacher/register")
    public String registerPage(Model model) {
        populateTurnstileModel(model);
        return "admin/register";
    }

    @PostMapping("/teacher/register")
    public String registerTeacher(@RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  @RequestParam(name = "cf-turnstile-response", required = false) String turnstileToken,
                                  HttpServletRequest request,
                                  Model model) {
        if (!bruteForceProtectionService.consumeRegisterAttempt(RequestFingerprint.clientIp(request))) {
            model.addAttribute("registerError", "Troppi tentativi ravvicinati. Riprova tra qualche minuto.");
            model.addAttribute("username", username);
            populateTurnstileModel(model);
            return "admin/register";
        }

        if (!turnstileCaptchaService.verifyToken(turnstileToken, RequestFingerprint.clientIp(request))) {
            model.addAttribute("registerError", "Verifica CAPTCHA non riuscita. Riprova.");
            model.addAttribute("username", username);
            populateTurnstileModel(model);
            return "admin/register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("registerError", "Le password non coincidono.");
            model.addAttribute("username", username);
            populateTurnstileModel(model);
            return "admin/register";
        }

        try {
            teacherAuthenticationService.register(username, password);
            return "redirect:/teacher/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registerError", ex.getMessage());
            model.addAttribute("username", username);
            populateTurnstileModel(model);
            return "admin/register";
        }
    }


    private void populateTurnstileModel(Model model) {
        model.addAttribute("turnstileEnabled", turnstileCaptchaService.isEnabled());
        model.addAttribute("turnstileSiteKey", turnstileCaptchaService.getSiteKey());
    }

    @GetMapping("/teacher")
    public String adminDashboard(Model model) {
        val currentTeacher = teacherAuthenticationService.getCurrentTeacher();
        model.addAttribute("quizzes", quizService.findAllForAdmin(currentTeacher));
        model.addAttribute("isAdmin", currentTeacher.isAdmin());
        if (currentTeacher.isAdmin()) {
            val shareTeachers = teacherAdministrationService.findAllTeachers(currentTeacher).stream()
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
        model.addAttribute("students", studentService.findAll(teacherAuthenticationService.getCurrentTeacher()));
        return "admin/students";
    }

    @GetMapping("/teacher/logs")
    public String adminLogs() {
        return "admin/logs";
    }

    @GetMapping("/teacher/results")
    public String adminResults(Model model) {
        val currentTeacher = teacherAuthenticationService.getCurrentTeacher();
        val results = quizSubmissionService.findAllResults(currentTeacher);
        val quizzesById = quizService.findAllForAdmin(currentTeacher).stream()
                .collect(Collectors.toMap(
                        QuizDto.Response::getId,
                        quiz -> quiz,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        val totalStudents = studentService.findAll(currentTeacher).size();

        val groupedResults = results.stream()
                .collect(Collectors.groupingBy(
                        QuizSubmissionService.ResultRow::quizId,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new QuizResultGroup(
                        entry.getKey(),
                        entry.getValue().getFirst().quizTitle(),
                        entry.getValue(),
                        buildAnalytics(entry.getKey(), entry.getValue(), quizzesById, totalStudents)))
                .toList();

        model.addAttribute("groupedResults", groupedResults);
        return "admin/results";
    }

    private QuizResultAnalytics buildAnalytics(UUID quizId,
                                               List<QuizSubmissionService.ResultRow> results,
                                               Map<UUID, QuizDto.Response> quizzesById,
                                               int totalStudents) {
        if (results == null || results.isEmpty()) {
            return new QuizResultAnalytics("0.0%", "0.0%", List.of(), "Non ci sono ancora abbastanza dati.");
        }

        val averageScorePct = results.stream()
                .mapToDouble(result -> {
                    if (result.totalQuestions() == null || result.totalQuestions() <= 0 || result.score() == null) {
                        return 0D;
                    }
                    return (double) result.score() / result.totalQuestions();
                })
                .average()
                .orElse(0D);

        val completionRatePct = totalStudents <= 0 ? 0D : (double) results.size() / totalStudents;

        val difficultQuestionsData = buildDifficultQuestions(quizId, results, quizzesById);
        val difficultQuestions = difficultQuestionsData.questions();
        val difficultQuestionsMessage = difficultQuestions.isEmpty()
                ? (difficultQuestionsData.hasAttempts()
                ? "Tutte le risposte erano corrette."
                : "Non ci sono ancora abbastanza dati.")
                : "";

        return new QuizResultAnalytics(
                String.format(Locale.ROOT, "%.1f%%", averageScorePct * 100),
                String.format(Locale.ROOT, "%.1f%%", completionRatePct * 100),
                difficultQuestions,
                difficultQuestionsMessage
        );
    }

    private DifficultQuestionsData buildDifficultQuestions(UUID quizId,
                                                           List<QuizSubmissionService.ResultRow> results,
                                                           Map<UUID, QuizDto.Response> quizzesById) {
        val quiz = quizzesById.get(quizId);
        if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            return new DifficultQuestionsData(List.of(), false);
        }

        val stats = new java.util.ArrayList<QuestionStats>(quiz.getQuestions().size());
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            stats.add(new QuestionStats(i, quiz.getQuestions().get(i).getText(), 0, 0));
        }

        for (val result : results) {
            if (result.answers() == null) {
                continue;
            }
            int max = Math.min(result.answers().size(), quiz.getQuestions().size());
            for (int i = 0; i < max; i++) {
                val given = result.answers().get(i);
                val expected = quiz.getQuestions().get(i).getAnswer();
                val current = stats.get(i);
                int attempts = current.attempts() + 1;
                int errors = current.errors() + (Objects.equals(given, expected) ? 0 : 1);
                stats.set(i, new QuestionStats(current.index(), current.text(), attempts, errors));
            }
        }

        val attemptedStats = stats.stream()
                .filter(stat -> stat.attempts() > 0);
        val difficultQuestions = attemptedStats
                .filter(stat -> stat.errors() > 0)
                .sorted((left, right) -> {
                    val leftRate = (double) left.errors() / left.attempts();
                    val rightRate = (double) right.errors() / right.attempts();
                    return Double.compare(rightRate, leftRate);
                })
                .limit(3)
                .map(stat -> new DifficultQuestion(
                        stat.index() + 1,
                        stat.text() == null || stat.text().isBlank() ? "Domanda senza testo" : stat.text(),
                        String.format(Locale.ROOT, "errori: %d/%d", stat.errors(), stat.attempts())
                ))
                .toList();
        val hasAttempts = stats.stream().anyMatch(stat -> stat.attempts() > 0);
        return new DifficultQuestionsData(difficultQuestions, hasAttempts);
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
            teacherAuthenticationService.changePassword(teacherAuthenticationService.getCurrentTeacher(), currentPassword, newPassword);
            model.addAttribute("profileSuccess", "Password aggiornata con successo.");
            return "admin/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("profileError", ex.getMessage());
            return "admin/profile";
        }
    }

    @GetMapping("/teacher/quiz/new")
    public String newQuiz(Model model) {
        model.addAttribute("aiEnabled", teacherAuthenticationService.getCurrentTeacher().isAiEnabled());
        model.addAttribute("quizQuestionsJson", "[]");
        return "admin/quiz-editor";
    }

    @GetMapping("/teacher/quiz/{id}/edit")
    public String editQuiz(@PathVariable UUID id, Model model) {
        model.addAttribute("aiEnabled", teacherAuthenticationService.getCurrentTeacher().isAiEnabled());
        val quiz = quizService.findByIdForTeacher(id, teacherAuthenticationService.getCurrentTeacher());
        model.addAttribute("quiz", quiz);
        model.addAttribute("quizQuestionsJson", serializeQuestions(quiz.getQuestions()));
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
        val currentTeacher = teacherAuthenticationService.getCurrentTeacher();
        val teachers = teacherAdministrationService.findAllTeachers(currentTeacher);
        model.addAttribute("teachers", teachers);
        model.addAttribute("currentTeacherId", currentTeacher.getId());
        return "admin/system-teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/admin")
    public String updateTeacherAdminFlag(@PathVariable UUID id,
                                         @RequestParam("admin") boolean admin) {
        ensureAdmin();
        teacherAdministrationService.updateTeacherAdminFlag(id, admin, teacherAuthenticationService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/ai")
    public String updateTeacherAiFlag(@PathVariable UUID id,
                                      @RequestParam("aiEnabled") boolean aiEnabled) {
        ensureAdmin();
        teacherAdministrationService.updateTeacherAiFlag(id, aiEnabled, teacherAuthenticationService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/enabled")
    public String updateTeacherEnabledFlag(@PathVariable UUID id,
                                           @RequestParam("enabled") boolean enabled) {
        ensureAdmin();
        teacherAdministrationService.updateTeacherEnabledFlag(id, enabled, teacherAuthenticationService.getCurrentTeacher());
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/reset-password")
    public String resetTeacherPassword(@PathVariable UUID id,
                                       RedirectAttributes redirectAttributes) {
        ensureAdmin();
        val temporaryPassword = teacherAdministrationService.resetTeacherPassword(id, teacherAuthenticationService.getCurrentTeacher());
        redirectAttributes.addFlashAttribute("teacherResetSuccess", String.format("Password resettata correttamente a %s", temporaryPassword));
        return "redirect:/teacher/system/teachers";
    }

    @PostMapping("/teacher/system/teachers/{id}/delete")
    public String deleteTeacher(@PathVariable UUID id) {
        ensureAdmin();
        teacherLifecycleService.deleteTeacherCompletely(id, teacherAuthenticationService.getCurrentTeacher());
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
        if (!teacherAuthenticationService.getCurrentTeacher().isAdmin()) {
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

    private String serializeQuestions(Object questions) {
        try {
            return objectMapper.writeValueAsString(questions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private record QuizResultGroup(UUID quizId,
                                   String quizTitle,
                                   List<QuizSubmissionService.ResultRow> results,
                                   QuizResultAnalytics analytics) {
    }

    private record QuizResultAnalytics(String averageScore,
                                       String completionRate,
                                       List<DifficultQuestion> difficultQuestions,
                                       String difficultQuestionsMessage) {
    }

    private record DifficultQuestion(int position, String text, String errorsSummary) {
    }

    private record DifficultQuestionsData(List<DifficultQuestion> questions, boolean hasAttempts) {
    }

    private record QuestionStats(int index, String text, int attempts, int errors) {
    }

    private record ShareTeacherOption(UUID id, String username) {
    }
}
