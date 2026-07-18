package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class QuestionAlreadyAnsweredException extends ApiException {
    public QuestionAlreadyAnsweredException(Long questionId) {
        super("QUESTION_ALREADY_ANSWERED", HttpStatus.CONFLICT,
              "Question " + questionId + " was already answered.");
    }
}
