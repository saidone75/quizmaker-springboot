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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.saidone.quizmaker.config.RequestFingerprint;
import org.saidone.quizmaker.service.BruteForceProtectionService;
import org.saidone.quizmaker.service.TeacherAuthenticationService;
import org.saidone.quizmaker.service.TurnstileCaptchaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TeacherAuthWebController {

    private final TeacherAuthenticationService teacherAuthenticationService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final TurnstileCaptchaService turnstileCaptchaService;

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
}
