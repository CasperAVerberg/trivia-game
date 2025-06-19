package com.example.trivia.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String id;              // Unique id for the question
    private String question;        // Question text
    private List<String> options;   // Shuffled list of all options
}