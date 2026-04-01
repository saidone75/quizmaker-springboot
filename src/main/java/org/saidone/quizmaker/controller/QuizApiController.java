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

    @GetMapping
    public ResponseEntity<List<QuizDto.Response>> getAll() {
        return ResponseEntity.ok(quizService.findPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.findPublishedById(id));
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
        quizSubmissionService.unlockQuizForStudent(studentId, quizId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{quizId}/unlock-all")
    public ResponseEntity<Integer> unlockAllForQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(quizSubmissionService.unlockAllForQuiz(quizId));
    }

    @PostMapping
    public ResponseEntity<QuizDto.Response> create(@Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizDto.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.ok(quizService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publication")
    public ResponseEntity<QuizDto.Response> updatePublicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.PublicationUpdateRequest request) {
        return ResponseEntity.ok(quizService.updatePublicationStatus(id, request.getPublished()));
    }

    @PostMapping(value = "/generate", consumes = {"multipart/form-data"})
    public ResponseEntity<QuizDto.Request> generateWithAi(
            @Valid @ModelAttribute QuizGenerationRequestDto request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        val attachmentText = documentTextExtractorService.extractText(file);
        return ResponseEntity.ok(openAiQuizGeneratorService.generateQuiz(request, attachmentText));
    }

}
