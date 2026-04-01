package org.saidone.quizmaker.repository;

import org.saidone.quizmaker.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByLoginKeywordIgnoreCase(String loginKeyword);

    boolean existsByLoginKeyword(String loginKeyword);

    List<Student> findAllByOrderByFullNameAsc();
}
