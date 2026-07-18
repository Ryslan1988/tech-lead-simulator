package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class NoQuestionAvailableException extends ApiException {
    public NoQuestionAvailableException(Long interviewId) {
        super("NO_QUESTION_AVAILABLE", HttpStatus.CONFLICT,
              "All rounds already answered for interview " + interviewId + ".");
    }
}
