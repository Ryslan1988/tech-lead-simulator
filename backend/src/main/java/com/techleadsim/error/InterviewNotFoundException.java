package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class InterviewNotFoundException extends ApiException {
    public InterviewNotFoundException(Long id) {
        super("INTERVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, "No interview session with id " + id + ".");
    }
}
