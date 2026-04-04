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

package org.saidone.quizmaker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UtilityClass
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
        private List<QuestionDto> questions;
        private Integer questionsCount;
        private LocalDateTime createdAt;
        private String createdByUsername;
        private LocalDateTime modifiedAt;
        private String modifiedByUsername;
        private Boolean published;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationUpdateRequest {
        @NotNull(message = "Il campo published è obbligatorio")
        private Boolean published;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareRequest {
        @NotNull(message = "La lista insegnanti è obbligatoria")
        @Size(min = 1, message = "Seleziona almeno un insegnante")
        private List<UUID> teacherIds;
    }

}
