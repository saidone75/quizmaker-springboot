package com.quizmaker.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDto {
    private String text;
    private String emoji;
    private List<String> options;
    private Integer answer;
    private String feedback;
}