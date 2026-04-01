package org.saidone.quizmaker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class QuizSubmissionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Le risposte sono obbligatorie")
        private List<Integer> answers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID quizId;
        private Integer score;
        private Integer total;
        private Boolean locked;
    }
}
