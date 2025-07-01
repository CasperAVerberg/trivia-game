package com.example.trivia.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestTemplate;

import com.example.trivia.model.Question;

class TriviaServiceTest {

    private TriviaService triviaService;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        triviaService = new TriviaService(restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldReturnListOfQuestionsWhenResponseIsValid() {
        String json = """
            {
              "response_code": 0,
              "results": [
                {
                  "question": "What is 2 + 2?",
                  "correct_answer": "4",
                  "incorrect_answers": ["3", "5", "22"]
                }
              ]
            }
            """;

        mockServer.expect(requestTo("https://opentdb.com/api.php?amount=10"))
                  .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<Question> questions = triviaService.fetchQuestions();

        assertEquals(1, questions.size());
        assertEquals("What is 2 + 2?", questions.get(0).getQuestion());
        assertEquals("4", questions.get(0).getCorrect_answer());
        assertEquals(3, questions.get(0).getIncorrect_answers().length);
    }

    
    @Test
    void shouldRetryWhenRateLimited() {
        String rateLimitedJson = "{\"response_code\":5,\"results\":[]}";
        String validJson = """
            {
              "response_code": 0,
              "results": [
                {
                  "question": "Retry success?",
                  "correct_answer": "Yes",
                  "incorrect_answers": ["No"]
                }
              ]
            }
            """;

        mockServer.expect(requestTo("https://opentdb.com/api.php?amount=10"))
                  .andRespond(withSuccess(rateLimitedJson, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://opentdb.com/api.php?amount=10"))
                  .andRespond(withSuccess(validJson, MediaType.APPLICATION_JSON));

        List<Question> questions = triviaService.fetchQuestions();

        assertEquals(1, questions.size());
        assertEquals("Retry success?", questions.get(0).getQuestion());
    }

    @Test
    void shouldFailAfterMaxRetries() {
        String rateLimitedJson = "{\"response_code\":5,\"results\":[]}";

        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("https://opentdb.com/api.php?amount=10"))
                      .andRespond(withSuccess(rateLimitedJson, MediaType.APPLICATION_JSON));
        }

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            triviaService.fetchQuestions();
        });

        assertTrue(exception.getMessage().contains("Trivia API fetch failed after retries"));
    }
}
