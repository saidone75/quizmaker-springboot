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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizGenerationRequestDto {

    @NotBlank(message = "L'argomento è obbligatorio")
    private String topic;

    @Min(value = 1, message = "Il numero di domande deve essere almeno 1")
    private Integer numberOfQuestions;

    @NotBlank(message = "La difficoltà è obbligatoria")
    private String difficulty;

    @NotBlank(message = "Il tono è obbligatorio")
    private String tone;
}
