package com.quizmaker;

import com.quizmaker.repository.QuizRepository;
import com.quizmaker.service.QuizService;
import com.quizmaker.dto.QuizDto;
import com.quizmaker.model.Quiz;
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

    @InjectMocks
    private QuizService quizService;

    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        sampleQuiz = Quiz.builder()
                .id(UUID.randomUUID())
                .title("Quiz di Test")
                .emoji("🧪")
                .questions("[{\"text\":\"Domanda?\",\"options\":[\"A\",\"B\"],\"answer\":0}]")
                .build();
    }

    @Test
    void findAll_returnsAllQuizzes() {
        when(quizRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleQuiz));
        List<QuizDto.Response> result = quizService.findAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Quiz di Test");
    }

    @Test
    void findById_returnsQuiz() {
        when(quizRepository.findById(sampleQuiz.getId())).thenReturn(Optional.of(sampleQuiz));
        QuizDto.Response result = quizService.findById(sampleQuiz.getId());
        assertThat(result.getId()).isEqualTo(sampleQuiz.getId());
        assertThat(result.getEmoji()).isEqualTo("🧪");
    }

    @Test
    void create_savesAndReturnsQuiz() {
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);
        QuizDto.Request request = QuizDto.Request.builder()
                .title("Quiz di Test")
                .emoji("🧪")
                .questions("[{\"text\":\"Domanda?\",\"options\":[\"A\",\"B\"],\"answer\":0}]")
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
