package com.quizmaker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class QuizDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Il titolo è obbligatorio")
        private String title;

        @NotBlank(message = "L'emoji è obbligatoria")
        private String emoji;

        @NotNull(message = "Le domande sono obbligatorie")
        private List<QuestionDto> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String title;
        private String emoji;
        private Boolean published;
        private List<QuestionDto> questions;
        private Integer questionsCount;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationUpdateRequest {
        @NotNull(message = "Il campo published è obbligatorio")
        private Boolean published;
    }

}
