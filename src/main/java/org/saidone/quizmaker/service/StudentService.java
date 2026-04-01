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

package org.saidone.quizmaker.service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.dto.StudentDto;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudentRepository studentRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    @Transactional(readOnly = true)
    public List<StudentDto.Response> findAll() {
        return studentRepository.findAllByOrderByFullNameAsc()
                .stream()
                .map(s -> new StudentDto.Response(s.getId(), s.getFullName(), s.getLoginKeyword()))
                .toList();
    }

    @Transactional
    public StudentDto.Response create(String fullName) {
        val cleanedName = fullName == null ? "" : fullName.trim();
        if (cleanedName.isBlank()) {
            throw new IllegalArgumentException("Il nome dello studente è obbligatorio");
        }

        val student = Student.builder()
                .id(UUID.randomUUID())
                .fullName(cleanedName)
                .build();

        return saveWithUniqueKeyword(student);
    }

    @Transactional
    public void delete(UUID studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Studente non trovato: " + studentId);
        }
        quizSubmissionRepository.deleteAllByStudentId(studentId);
        studentRepository.deleteById(studentId);
    }

    @Transactional
    public StudentDto.Response regenerateLoginKeyword(UUID studentId) {
        val student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Studente non trovato: " + studentId));
        return saveWithUniqueKeyword(student);
    }

    @Transactional
    public int regenerateAllLoginKeywords() {
        val students = studentRepository.findAll();
        students.forEach(this::saveWithUniqueKeyword);
        return students.size();
    }

    private StudentDto.Response saveWithUniqueKeyword(Student student) {
        for (int attempt = 0; attempt < 100; attempt++) {
            val newLoginKeyword = randomAlphanumeric(4);
            if (!studentRepository.existsByLoginKeyword(newLoginKeyword)) {
                student.setLoginKeyword(newLoginKeyword);
                val saved = studentRepository.saveAndFlush(student);
                return new StudentDto.Response(saved.getId(), saved.getFullName(), saved.getLoginKeyword());
            }
        }
        throw new IllegalStateException("Impossibile generare una keyword univoca. Riprova.");
    }

    public static String randomAlphanumeric(int length) {
        val sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
