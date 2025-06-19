package com.example.trivia.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.trivia.model.Question;

@Service
public class TriviaService {

    private static final String OpenTriviaApiURL = "https://opentdb.com/api.php?amount=10";

    public List<Question> fetchQuestions() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(OpenTriviaApiURL, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        return results.stream().map(this::mapToQuestion).toList();
    }

    private Question mapToQuestion(Map<String, Object> map) {
        Question question = new Question();
        question.setQuestion((String) map.get("question"));
        question.setCorrect_answer((String) map.get("correct_answer"));
        List<String> incorrectsAnswers = (List<String>) map.get("incorrect_answers");
        question.setIncorrect_answers(incorrectsAnswers.toArray(new String[0]));
        return question;
    }
}
