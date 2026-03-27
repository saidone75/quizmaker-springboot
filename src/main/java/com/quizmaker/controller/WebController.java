package com.quizmaker.controller;

import com.quizmaker.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final QuizService quizService;

    // Students page (default)
    @GetMapping("/")
    public String studentPage(Model model) {
        model.addAttribute("quizzes", quizService.findAll());
        return "student";
    }

    // Admin page - login
    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    // Admin page - dashboard
    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("quizzes", quizService.findAll());
        return "admin/dashboard";
    }

    // Admin page - create quiz
    @GetMapping("/admin/quiz/new")
    public String newQuiz() {
        return "admin/quiz-editor";
    }

    // Admin page - modify quiz
    @GetMapping("/admin/quiz/{id}/edit")
    public String editQuiz(@org.springframework.web.bind.annotation.PathVariable UUID id, Model model) {
        model.addAttribute("quiz", quizService.findById(id));
        return "admin/quiz-editor";
    }

}
