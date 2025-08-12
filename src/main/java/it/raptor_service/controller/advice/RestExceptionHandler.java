package it.raptor_service.controller.advice;

import it.raptor_service.exception.InvalidFileException;
import it.raptor_service.model.JobSubmissionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InvalidFileException.class)
    protected ResponseEntity<JobSubmissionResponse> handleInvalidFile(InvalidFileException ex) {
        return ResponseEntity.badRequest().body(new JobSubmissionResponse(null, "FAILED", ex.getMessage()));
    }
}
