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
