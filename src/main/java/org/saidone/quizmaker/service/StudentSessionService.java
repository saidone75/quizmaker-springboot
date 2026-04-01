package org.saidone.quizmaker.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StudentSessionService {

    public static final String STUDENT_ID_SESSION_KEY = "STUDENT_ID";

    private final StudentRepository studentRepository;

    public Optional<Student> getLoggedStudent(HttpSession session) {
        val studentId = session.getAttribute(STUDENT_ID_SESSION_KEY);
        if (studentId == null) {
            return Optional.empty();
        }
        return studentRepository.findById(UUID.fromString(String.valueOf(studentId)));
    }

    public Optional<Student> login(HttpSession session, String keyword) {
        return studentRepository.findByLoginKeywordIgnoreCase(keyword)
                .map(student -> {
                    session.setAttribute(STUDENT_ID_SESSION_KEY, student.getId().toString());
                    return student;
                });
    }

    public void logout(HttpSession session) {
        session.removeAttribute(STUDENT_ID_SESSION_KEY);
    }
}
