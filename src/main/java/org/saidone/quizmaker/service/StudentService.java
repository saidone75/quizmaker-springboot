package org.saidone.quizmaker.service;

import lombok.RequiredArgsConstructor;
import org.saidone.quizmaker.dto.StudentDto;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
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
        String cleanedName = fullName == null ? "" : fullName.trim();
        if (cleanedName.isBlank()) {
            throw new IllegalArgumentException("Il nome dello studente è obbligatorio");
        }

        Student student = Student.builder()
                .id(UUID.randomUUID())
                .fullName(cleanedName)
                .loginKeyword(generateUniqueKeyword())
                .build();

        Student saved = studentRepository.save(student);
        return new StudentDto.Response(saved.getId(), saved.getFullName(), saved.getLoginKeyword());
    }

    @Transactional
    public void delete(UUID studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Studente non trovato: " + studentId);
        }
        quizSubmissionRepository.deleteAllByStudentId(studentId);
        studentRepository.deleteById(studentId);
    }

    private String generateUniqueKeyword() {
        for (int attempt = 0; attempt < 100; attempt++) {
            String keyword = randomCode(6).toUpperCase(Locale.ROOT);
            if (!studentRepository.existsByLoginKeywordIgnoreCase(keyword)) {
                return keyword;
            }
        }
        throw new IllegalStateException("Impossibile generare una keyword univoca. Riprova.");
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
