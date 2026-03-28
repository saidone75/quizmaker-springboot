package com.quizmaker;

import com.quizmaker.dto.QuestionDto;
import com.quizmaker.mapper.QuestionMapper;
import com.quizmaker.mapper.QuizMapper;
import com.quizmaker.entity.Question;
import com.quizmaker.repository.QuizRepository;
import com.quizmaker.service.QuizService;
import com.quizmaker.dto.QuizDto;
import com.quizmaker.entity.Quiz;
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
    private QuizMapper quizMapper;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuizService quizService;

    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        val question = new Question();
        question.setText("question");
        question.setOptions(List.of("A", "B"));
        question.setAnswer(0);
        sampleQuiz = Quiz.builder()
                .id(UUID.randomUUID())
                .title("Quiz di Test")
                .emoji("🧪")
                .questions(List.of(question))
                .build();
    }

    @Test
    void findAll_returnsAllQuizzes() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);
        List<QuizDto.Response> result = quizService.findAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Quiz di Test");
    }

    @Test
    void findById_returnsQuiz() {
        val response = QuizDto.Response.builder()
                .id(sampleQuiz.getId())
                .title(sampleQuiz.getTitle())
                .emoji(sampleQuiz.getEmoji())
                .questionsCount(sampleQuiz.getQuestions().size())
                .build();
        when(quizRepository.findById(sampleQuiz.getId())).thenReturn(Optional.of(sampleQuiz));
        when(quizMapper.toResponse(sampleQuiz)).thenReturn(response);
        QuizDto.Response result = quizService.findById(sampleQuiz.getId());
        assertThat(result.getId()).isEqualTo(sampleQuiz.getId());
        assertThat(result.getEmoji()).isEqualTo("🧪");
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
        QuizDto.Request request = QuizDto.Request.builder()
                .title("Quiz di Test")
                .emoji("🧪")
                .questions(List.of(questionDto))
                .build();
        QuizDto.Response result = quizService.create(request);
        assertThat(result.getTitle()).isEqualTo("Quiz di Test");
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void delete_callsRepository() {
        when(quizRepository.existsById(sampleQuiz.getId())).thenReturn(true);
        quizService.delete(sampleQuiz.getId());
        verify(quizRepository, times(1)).deleteById(sampleQuiz.getId());
    }

}
