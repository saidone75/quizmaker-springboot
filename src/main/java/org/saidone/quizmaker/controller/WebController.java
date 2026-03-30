package org.saidone.quizmaker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.service.QuizSubmissionService;
import org.saidone.quizmaker.service.StudentService;
import org.saidone.quizmaker.service.StudentSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final QuizService quizService;
    private final QuizSubmissionService quizSubmissionService;
    private final StudentSessionService studentSessionService;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String studentPage(HttpSession session, Model model) {
        val maybeStudent = studentSessionService.getLoggedStudent(session);
        if (maybeStudent.isEmpty()) {
            return "student-login";
        }

        val quizzes = quizService.findPublished();
        Set<UUID> lockedQuizIds = quizSubmissionService.findLockedQuizIdsForStudent(maybeStudent.get());

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

    @GetMapping("/admin/results")
    public String adminResults(Model model) {
        model.addAttribute("results", quizSubmissionService.findAllResults());
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

}
