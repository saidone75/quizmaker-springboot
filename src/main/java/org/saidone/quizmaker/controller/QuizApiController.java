package org.saidone.quizmaker.controller;

import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.dto.QuizGenerationRequestDto;
import org.saidone.quizmaker.service.DocumentTextExtractorService;
import org.saidone.quizmaker.service.OpenAiQuizGeneratorService;
import org.saidone.quizmaker.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final OpenAiQuizGeneratorService openAiQuizGeneratorService;
    private final DocumentTextExtractorService documentTextExtractorService;

    // PUBLIC: students can read all quizzes
    @GetMapping
    public ResponseEntity<List<QuizDto.Response>> getAll() {
        return ResponseEntity.ok(quizService.findPublished());
    }

    // PUBLIC: students can read a single quiz
    @GetMapping("/{id}")
    public ResponseEntity<QuizDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.findPublishedById(id));
    }

    // PROTECTED: only teacher can create
    @PostMapping
    public ResponseEntity<QuizDto.Response> create(@Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(request));
    }

    // PROTECTED: only teacher can modify
    @PutMapping("/{id}")
    public ResponseEntity<QuizDto.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.Request request) {
        return ResponseEntity.ok(quizService.update(id, request));
    }

    // PROTECTED: only teacher can delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // PROTECTED: only teacher can publish/unpublish
    @PutMapping("/{id}/publication")
    public ResponseEntity<QuizDto.Response> updatePublicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuizDto.PublicationUpdateRequest request) {
        return ResponseEntity.ok(quizService.updatePublicationStatus(id, request.getPublished()));
    }

    // PROTECTED: only teacher can generate using AI
    @PostMapping(value = "/generate", consumes = {"multipart/form-data"})
    public ResponseEntity<QuizDto.Request> generateWithAi(
            @Valid @ModelAttribute QuizGenerationRequestDto request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String attachmentText = documentTextExtractorService.extractText(file);
        return ResponseEntity.ok(openAiQuizGeneratorService.generateQuiz(request, attachmentText));
    }

}
