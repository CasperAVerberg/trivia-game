package com.example.trivia.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.trivia.model.Question;

 /* Service responsible for fetching trivia questions from the Open Trivia Database API.
  Includes retry logic to handle rate limiting (response_code 5). */
@Service
public class TriviaService {
    private static final String OPEN_TRIVIA_API_URL = "https://opentdb.com/api.php?amount=10";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    private final RestTemplate restTemplate;

    public TriviaService() {
        this.restTemplate = new RestTemplate();
    }

    public TriviaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Fetches a list of trivia questions from the Open Trivia DB API.
    public List<Question> fetchQuestions() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Send GET request to the API
                Map<String, Object> response = restTemplate.getForObject(OPEN_TRIVIA_API_URL, Map.class);

                if (response == null) {
                    throw new RuntimeException("Null response from trivia API");
                }

                Integer responseCode = (Integer) response.get("response_code");

                // If API is rate-limiting us (code 5), wait and retry
                if (responseCode != null && responseCode == 5) {
                    System.out.println("⚠️ Rate limited by API (code 5). Retrying in 5 seconds...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                if (responseCode != null && responseCode != 0) {
                    throw new RuntimeException("Trivia API returned error code: " + responseCode);
                }

                //  to extract the results field from the response
                Object resultsObj = response.get("results");
                if (!(resultsObj instanceof List<?> rawResults)) {
                    throw new RuntimeException("Unexpected format for results");
                }

                return rawResults.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> mapToQuestion((Map<String, Object>) item))
                        .toList();

            // Error handling
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during API retry", e);

            } catch (RuntimeException e) {
                if (attempt == MAX_RETRIES) {
                    System.err.println("❌ Max retries reached. Trivia API failed.");
                    throw new RuntimeException("Trivia API fetch failed after retries", e);
                }
                System.err.printf("Attempt %d failed: %s%n", attempt, e.getMessage());
            }
        }
        throw new RuntimeException("Unreachable: all retries failed");
    }

    private Question mapToQuestion(Map<String, Object> map) {
        Question question = new Question();

        // Extract question and answers
        question.setQuestion((String) map.getOrDefault("question", "No question"));
        question.setCorrect_answer((String) map.getOrDefault("correct_answer", "N/A"));

        Object incorrects = map.get("incorrect_answers");
        if (incorrects instanceof List<?> list) {
            question.setIncorrect_answers(list.toArray(String[]::new)); 
        } else {
            System.err.println("Invalid or missing incorrect_answers: " + map);
            question.setIncorrect_answers(new String[0]); 
        }

        return question;
    }
}
