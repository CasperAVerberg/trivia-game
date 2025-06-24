package com.example.trivia.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.trivia.model.Question;

@Service
public class TriviaService {

    private static final String OpenTriviaApiURL = "https://opentdb.com/api.php?amount=10";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    // Get Questions from Open Trivia API
    public List<Question> fetchQuestions() {
        RestTemplate restTemplate = new RestTemplate();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> response = restTemplate.getForObject(OpenTriviaApiURL, Map.class);
                
                // Error handling if response is empty
                if (response == null) {
                    throw new RuntimeException("Null response from trivia API");
                }

                Integer responseCode = (Integer) response.get("response_code");
                
                // Wait 5 seconds before retry'ing
                if (responseCode != null && responseCode == 5) {
                    System.out.println("Rate limited by API (response_code 5), retrying in 5 seconds...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }
                
                // Different response than Rate limited
                if (responseCode != null && responseCode != 0) {
                    throw new RuntimeException("Trivia API returned error code: " + responseCode);
                }

                // Create results object
                Object resultsObj = response.get("results");
                if (!(resultsObj instanceof List<?> rawResults)) {
                    throw new RuntimeException("Unexpected format for results");
                }

                return rawResults.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> mapToQuestion((Map<String, Object>) item))
                        .toList();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                throw new RuntimeException("Interrupted while waiting to retry API call", e);
            } catch (RuntimeException e) {
                if (attempt == MAX_RETRIES) {
                    System.err.println("Max retries reached. Trivia API failed.");
                    throw new RuntimeException("Trivia API fetch failed after retries", e);
                }
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
            }
        }

        throw new RuntimeException("Unreachable code: Trivia fetch failed");
    }

    // Encode the questions and answers
    private Question mapToQuestion(Map<String, Object> map) {
        Question question = new Question();
        question.setQuestion((String) map.getOrDefault("question", "No question"));
        question.setCorrect_answer((String) map.getOrDefault("correct_answer", "N/A"));

        Object incorrects = map.get("incorrect_answers");
        if (incorrects instanceof List<?> list) {
            question.setIncorrect_answers(list.toArray(new String[0]));
        } else {
            System.err.println("Invalid or missing incorrect_answers: " + map);
            question.setIncorrect_answers(new String[0]);
        }

        return question;
    }
}