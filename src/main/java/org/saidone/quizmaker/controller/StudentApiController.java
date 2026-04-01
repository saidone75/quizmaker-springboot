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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.saidone.quizmaker.dto.StudentDto;
import org.saidone.quizmaker.service.StudentService;
import org.saidone.quizmaker.service.TeacherAuthService;
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
    private final TeacherAuthService teacherAuthService;

    @GetMapping
    public ResponseEntity<List<StudentDto.Response>> getAll() {
        return ResponseEntity.ok(studentService.findAll(teacherAuthService.getCurrentTeacher()));
    }

    @PostMapping
    public ResponseEntity<StudentDto.Response> create(@Valid @RequestBody StudentDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request.getFullName(), teacherAuthService.getCurrentTeacher()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id, teacherAuthService.getCurrentTeacher());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-password")
    public ResponseEntity<StudentDto.Response> regeneratePassword(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.regenerateLoginKeyword(id, teacherAuthService.getCurrentTeacher()));
    }

    @PostMapping("/regenerate-passwords")
    public ResponseEntity<Integer> regenerateAllPasswords() {
        return ResponseEntity.ok(studentService.regenerateAllLoginKeywords(teacherAuthService.getCurrentTeacher()));
    }
}
