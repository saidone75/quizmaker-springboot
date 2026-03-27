package com.quizmaker.controller;

import com.quizmaker.dto.QuizDto;
import com.quizmaker.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;

    // PUBLIC: students can read all quizzes
    @GetMapping
    public ResponseEntity<List<QuizDto.Response>> getAll() {
        return ResponseEntity.ok(quizService.findAll());
    }

    // PUBLIC: students can read a single quiz
    @GetMapping("/{id}")
    public ResponseEntity<QuizDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.findById(id));
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

}
