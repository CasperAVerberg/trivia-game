package com.example.trivia.model;

import java.util.Map;

import lombok.Data;

@Data
public class CheckAnswersRequest {
    private Map<String, String> userAnswers;
}