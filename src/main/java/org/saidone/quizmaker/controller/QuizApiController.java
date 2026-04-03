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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.dto.QuizGenerationRequestDto;
import org.saidone.quizmaker.dto.QuizSubmissionDto;
import org.saidone.quizmaker.service.DocumentTextExtractorService;
import org.saidone.quizmaker.service.OpenAiQuizGeneratorService;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.service.QuizSubmissionService;
import org.saidone.quizmaker.service.StudentSessionService;
import org.saidone.quizmaker.service.TeacherAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;
    private final QuizSubmissionService quizSubmissionService;
    private final StudentSessionService studentSessionService;
    private final OpenAiQuizGeneratorService openAiQuizGeneratorService;
    private final DocumentTextExtractorService documentTextExtractorService;
    private final TeacherAuthService teacherAuthService;

    @GetMapping
    public ResponseEntity<List<QuizDto.Response>> getAll(HttpSession session) {
        val student = studentSessionService.getLoggedStudent(session)
                .orElseThrow(() -> new IllegalStateException("Studente non autenticato"));
        return ResponseEntity.ok(quizService.findPublishedForTeacher(student.getTeacher()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDto.Response> getById(@PathVariable UUID id, HttpSession session) {
        val student = studentSessionService.getLoggedStudent(session)
                .orElseThrow(() -> new IllegalStateException("Studente non autenticato"));
        return ResponseEntity.ok(quizService.findPublishedByIdForTeacher(id, student.getTeacher()));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizSubmissionDto.Response> submit(
            @PathVariable UUID id,
            @Valid @RequestBody QuizSubmissionDto.Request request,
            HttpSession session) {
        val student = studentSessionService.getLoggedStudent(session)
                .orElseThrow(() -> new IllegalStateException("Studente non autenticato"));
        return ResponseEntity.ok(quizSubmissionService.submit(id, student, request.getAnswers()));
    }

    @PostMapping("/{quizId}/unlock/{studentId}")
    public ResponseEntity<Void> unlockQuiz(
            @PathVariable UUID quizId,
            @PathVariable UUID studentId) {
        quizSubmissionService.unlockQuizForStudent(studentId, quizId, teacherAuthService.getCurrentTeacher());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{quizId}/unlock-all")
    public ResponseEntity<Integer> unlockAllForQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(quizSubmissionService.unlockAllForQuiz(quizId, teacherAuthService.getCurrentTeacher()));
    }

    @PostMapping
    public ResponseEntity<QuizDto.Response> create(@Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(request, teacherAuthService.getCurrentTeacher()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizDto.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.ok(quizService.update(id, request, teacherAuthService.getCurrentTeacher()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        quizService.delete(id, teacherAuthService.getCurrentTeacher());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publication")
    public ResponseEntity<QuizDto.Response> updatePublicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.PublicationUpdateRequest request) {
        return ResponseEntity.ok(quizService.updatePublicationStatus(id, request.getPublished(), teacherAuthService.getCurrentTeacher()));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Integer> shareQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.ShareRequest request) {
        return ResponseEntity.ok(quizService.shareQuizToTeachers(id, request.getTeacherIds(), teacherAuthService.getCurrentTeacher()));
    }

    @PostMapping(value = "/generate", consumes = {"multipart/form-data"})
    public ResponseEntity<QuizDto.Request> generateWithAi(
            @Valid @ModelAttribute QuizGenerationRequestDto request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        val currentTeacher = teacherAuthService.getCurrentTeacher();
        if (!currentTeacher.isAiEnabled()) {
            throw new IllegalStateException("Generazione AI disabilitata per questo insegnante.");
        }
        val attachmentText = documentTextExtractorService.extractText(file);
        return ResponseEntity.ok(openAiQuizGeneratorService.generateQuiz(request, attachmentText));
    }

}
