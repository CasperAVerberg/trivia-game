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

    // Base URL to request trivia questions (10 multiple-choice questions)
    private static final String OPEN_TRIVIA_API_URL = "https://opentdb.com/api.php?amount=10";

    // Max number of retry attempts if the API returns a rate limit response (code 5)
    private static final int MAX_RETRIES = 3;

    // Time to wait (in milliseconds) between retry attempts
    private static final int RETRY_DELAY_MS = 5000;

    // HTTP client used to make the API request
    private final RestTemplate restTemplate;

    
    // Default constructor – creates a new RestTemplate instance for real HTTP requests.  
    public TriviaService() {
        this.restTemplate = new RestTemplate();
    }

    /* Constructor for dependency injection 
    Used to unit test by injecting a mock RestTemplate. */
    public TriviaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /* Fetches a list of trivia questions from the API.
    Implements retry logic if the API responds with a rate limit code (5). */
    
    public List<Question> fetchQuestions() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Send GET request to the API
                Map<String, Object> response = restTemplate.getForObject(OPEN_TRIVIA_API_URL, Map.class);

                if (response == null) {
                    throw new RuntimeException("Null response from trivia API");
                }

                // Read the response_code to determine the API status
                Integer responseCode = (Integer) response.get("response_code");

                // If API is rate-limiting us (code 5), wait and retry
                if (responseCode != null && responseCode == 5) {
                    System.out.println("⚠️ Rate limited by API (code 5). Retrying in 5 seconds...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // If any other non-successful response code is returned, fail early
                if (responseCode != null && responseCode != 0) {
                    throw new RuntimeException("Trivia API returned error code: " + responseCode);
                }

                // Attempt to extract the "results" field from the response
                Object resultsObj = response.get("results");
                if (!(resultsObj instanceof List<?> rawResults)) {
                    throw new RuntimeException("Unexpected format for results");
                }

                // Convert raw API results to List<Question>
                return rawResults.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> mapToQuestion((Map<String, Object>) item))
                        .toList();

            } catch (InterruptedException e) {
                // Ensure thread is re-marked as interrupted and throw wrapped exception
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during API retry", e);

            } catch (RuntimeException e) {
                // If final attempt fails, log and rethrow
                if (attempt == MAX_RETRIES) {
                    System.err.println("❌ Max retries reached. Trivia API failed.");
                    throw new RuntimeException("Trivia API fetch failed after retries", e);
                }
                // Otherwise, print failed attempt number and retry
                System.err.printf("Attempt %d failed: %s%n", attempt, e.getMessage());
            }
        }

        // This point should never be reached due to retry or early return
        throw new RuntimeException("Unreachable: all retries failed");
    }


    // Helper method to convert a raw API response Map into a strongly-typed Question object.
    private Question mapToQuestion(Map<String, Object> map) {
        Question question = new Question();

        // Extract question text and correct answer
        question.setQuestion((String) map.getOrDefault("question", "No question"));
        question.setCorrect_answer((String) map.getOrDefault("correct_answer", "N/A"));

        // Extract incorrect answers (array of strings)
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
