package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super("BAD_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
