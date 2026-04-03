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

package org.saidone.quizmaker.repository;

import org.saidone.quizmaker.entity.QuizSubmission;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, UUID> {

    Optional<QuizSubmission> findByStudentIdAndQuizId(UUID studentId, UUID quizId);

    List<QuizSubmission> findByStudent(Student student);

    List<QuizSubmission> findAllByStudentTeacherOrderBySubmittedAtDesc(Teacher teacher);

    List<QuizSubmission> findByQuizIdAndUnlockedFalseAndQuizTeacher(UUID quizId, Teacher teacher);

    Optional<QuizSubmission> findByStudentIdAndQuizIdAndStudentTeacherAndQuizTeacher(UUID studentId, UUID quizId, Teacher studentTeacher, Teacher quizTeacher);

    void deleteAllByQuizIdAndQuizTeacher(UUID quizId, Teacher teacher);

    void deleteAllByStudentIdAndStudentTeacher(UUID studentId, Teacher teacher);

    void deleteAllByStudentTeacherOrQuizTeacher(Teacher studentTeacher, Teacher quizTeacher);
}
