package org.saidone.quizmaker.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.saidone.quizmaker.dto.StudentDto;
import org.saidone.quizmaker.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<List<StudentDto.Response>> getAll() {
        return ResponseEntity.ok(studentService.findAll());
    }

    @PostMapping
    public ResponseEntity<StudentDto.Response> create(@Valid @RequestBody StudentDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request.getFullName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-password")
    public ResponseEntity<StudentDto.Response> regeneratePassword(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.regenerateLoginKeyword(id));
    }

    @PostMapping("/regenerate-passwords")
    public ResponseEntity<Integer> regenerateAllPasswords() {
        return ResponseEntity.ok(studentService.regenerateAllLoginKeywords());
    }
}
