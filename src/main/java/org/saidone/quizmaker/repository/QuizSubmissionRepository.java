package org.saidone.quizmaker.repository;

import org.saidone.quizmaker.entity.QuizSubmission;
import org.saidone.quizmaker.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, UUID> {

    Optional<QuizSubmission> findByStudentIdAndQuizId(UUID studentId, UUID quizId);

    List<QuizSubmission> findByStudent(Student student);

    List<QuizSubmission> findAllByOrderBySubmittedAtDesc();

    List<QuizSubmission> findByQuizIdAndUnlockedFalse(UUID quizId);

    void deleteAllByQuizId(UUID quizId);

    void deleteAllByStudentId(UUID studentId);
}
