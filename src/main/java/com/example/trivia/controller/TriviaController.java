package com.example.trivia.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.trivia.dto.QuestionDTO;
import com.example.trivia.model.CheckAnswersRequest;
import com.example.trivia.model.CheckAnswersResponse;
import com.example.trivia.model.Question;
import com.example.trivia.service.TriviaService;

import jakarta.servlet.http.HttpSession;

@RestController
public class TriviaController {

    @Autowired
    private TriviaService triviaService;

    // GET /questions
    @GetMapping("/questions")
    public List<QuestionDTO> getQuestions(HttpSession session) {
        List<Question> questions = triviaService.fetchQuestions();

        // Store questions (with answers) in session
        session.setAttribute("questions", questions);

        // Convert questions to QuestionDTOs without answers
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        for (Question q : questions) {
            List<String> options = new ArrayList<>(List.of(q.getIncorrect_answers()));
            options.add(q.getCorrect_answer());
            Collections.shuffle(options);
            String id = String.valueOf(q.getQuestion().hashCode());

            questionDTOs.add(new QuestionDTO(id, q.getQuestion(), options));
        }

        return questionDTOs;
    }

    // POST /checkanswers
    @PostMapping("/checkanswers")
    public CheckAnswersResponse checkAnswers(@RequestBody CheckAnswersRequest request, HttpSession session) {
        // Retrieve questions from session
        List<Question> questions = (List<Question>) session.getAttribute("questions");

        Map<String, Boolean> results = new HashMap<>();

        if (questions == null) {
            // No questions in session - error
            results.put("error", false);
        } else {
            Map<String, Question> questionMap = new HashMap<>();
            for (Question q : questions) {
                questionMap.put(String.valueOf(q.getQuestion().hashCode()), q);
            }

            // Check each user answer against the correct answer
            for (Map.Entry<String, String> entry : request.getUserAnswers().entrySet()) {
                String questionId = entry.getKey();
                String userAnswer = entry.getValue();

                Question question = questionMap.get(questionId);
                if (question != null) {
                    boolean isCorrect = question.getCorrect_answer().equalsIgnoreCase(userAnswer);
                    results.put(questionId, isCorrect);
                } else {
                    results.put(questionId, false);
                }
            }
        }

        CheckAnswersResponse response = new CheckAnswersResponse();
        response.setResults(results);
        return response;
    }
}
