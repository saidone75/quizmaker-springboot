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

package org.saidone.quizmaker;

import org.saidone.quizmaker.dto.QuestionDto;
import org.saidone.quizmaker.mapper.QuestionMapper;
import org.saidone.quizmaker.mapper.QuizMapper;
import org.saidone.quizmaker.entity.Question;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.service.QuizService;
import org.saidone.quizmaker.dto.QuizDto;
import org.saidone.quizmaker.entity.Quiz;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;

    @Mock
    private QuizMapper quizMapper;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuizService quizService;

    private Quiz sampleQuiz;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        val question = new Question();
        question.setText("question");
        question.setOptions(List.of("A", "B"));
        question.setAnswer(0);
        teacher = Teacher.builder().id(UUID.randomUUID()).username("teacher").password("pwd").build();

        sampleQuiz = Quiz.builder()
                .id(UUID.randomUUID())
                .title("Quiz di Test")
                .emoji("🧪")
                .published(true)
                .questions(List.of(question))
                .teacher(teacher)
                .build();
    }

    @Test
    void findAllForAdmin_returnsAllQuizzes() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findAllByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);
        val result = quizService.findAllForAdmin(teacher);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Quiz di Test");
    }

    @Test
    void findPublished_returnsOnlyPublishedQuizzes() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .published(sampleQuiz.getPublished())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findByTeacherAndPublishedTrueOrderByCreatedAtDesc(teacher)).thenReturn(List.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);

        val result = quizService.findPublishedForTeacher(teacher);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPublished()).isTrue();
    }

    @Test
    void findById_returnsQuiz() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findByIdAndTeacher(sampleQuiz.getId(), teacher)).thenReturn(Optional.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);
        val result = quizService.findByIdForTeacher(sampleQuiz.getId(), teacher);
        assertThat(result.getId()).isEqualTo(sampleQuiz.getId());
        assertThat(result.getEmoji()).isEqualTo("🧪");
    }

    @Test
    void findPublishedById_returnsOnlyPublishedQuiz() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .published(sampleQuiz.getPublished())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findByIdAndTeacherAndPublishedTrue(sampleQuiz.getId(), teacher)).thenReturn(Optional.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);

        val result = quizService.findPublishedByIdForTeacher(sampleQuiz.getId(), teacher);

        assertThat(result.getPublished()).isTrue();
    }

    @Test
    void create_savesAndReturnsQuiz() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);
        val questionDto = new QuestionDto();
        questionDto.setText("question");
        questionDto.setOptions(List.of("A", "B"));
        questionDto.setAnswer(0);
        val mappedQuestion = new Question();
        mappedQuestion.setText("question");
        mappedQuestion.setOptions(List.of("A", "B"));
        mappedQuestion.setAnswer(0);
        when(questionMapper.toEntity(questionDto)).thenReturn(mappedQuestion);
        val request = QuizDto.Request.builder()
                .title("Quiz di Test")
                .emoji("🧪")
                .questions(List.of(questionDto))
                .build();
        val result = quizService.create(request, teacher);
        assertThat(result.getTitle()).isEqualTo("Quiz di Test");
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void delete_callsRepository() {
        when(quizRepository.findByIdAndTeacher(sampleQuiz.getId(), teacher)).thenReturn(Optional.of(sampleQuiz));
        quizService.delete(sampleQuiz.getId(), teacher);
        verify(quizSubmissionRepository, times(1)).deleteAllByQuizIdAndQuizTeacher(sampleQuiz.getId(), teacher);
        verify(quizRepository, times(1)).delete(sampleQuiz);
    }

    @Test
    void updatePublicationStatus_updatesAndReturnsQuiz() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .published(false)
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();

        when(quizRepository.findByIdAndTeacher(sampleQuiz.getId(), teacher)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quizMapper.toResponse(any(Quiz.class))).thenReturn(response);

        val result = quizService.updatePublicationStatus(sampleQuiz.getId(), false, teacher);

        assertThat(result.getPublished()).isFalse();
        assertThat(sampleQuiz.getPublished()).isFalse();
        verify(quizRepository, times(1)).save(sampleQuiz);
    }

}
