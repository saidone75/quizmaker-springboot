package com.quizmaker.entity;

import lombok.Data;

import java.util.List;

@Data
public class Question {
    private String text;
    private String emoji;
    private List<String> options;
    private Integer answer;
    private String feedback;
}