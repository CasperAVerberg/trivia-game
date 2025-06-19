package com.example.trivia.model;

import lombok.Data;

@Data
public class Question {
    private String question;
    private String correct_answer;
    private String[] incorrect_answers;
}