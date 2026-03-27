package com.quizmaker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
        private String questions; // JSON string
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String title;
        private String emoji;
        private String questions; // JSON string
        private Integer questionsCount;
        private LocalDateTime createdAt;
    }

}