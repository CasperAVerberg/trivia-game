package com.example.trivia.model;

import java.util.Map;

import lombok.Data;

@Data
public class CheckAnswersResponse {
    private Map<String, Boolean> results; // questionId -> true/false
}