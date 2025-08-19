package it.raptor_service.service.validator;


import it.raptor_service.web.rest.RaptorController.ProcessRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RaptorRequestValidator implements ConstraintValidator<ValidRaptorRequest, ProcessRequest> {

    private static final int MAX_TEXT_LENGTH = 1000000;
    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 10000;
    private static final int MIN_LEVELS = 1;
    private static final int MAX_LEVELS = 10;

    @Override
    public boolean isValid(ProcessRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean valid = true;

        // Disable default message
        context.disableDefaultConstraintViolation();

        // Validate text length
        if (request.getText() != null && request.getText().length() > MAX_TEXT_LENGTH) {
            context.buildConstraintViolationWithTemplate("Text too long. Maximum allowed: " + MAX_TEXT_LENGTH + " characters")
                   .addPropertyNode("text")
                   .addConstraintViolation();
            valid = false;
        }

        // Validate chunkSize range (if present)
        Integer chunkSize = request.getChunkSize();
        if (chunkSize != null && (chunkSize < MIN_CHUNK_SIZE || chunkSize > MAX_CHUNK_SIZE)) {
            context.buildConstraintViolationWithTemplate("Chunk size must be between " + MIN_CHUNK_SIZE + " and " + MAX_CHUNK_SIZE)
                   .addPropertyNode("chunkSize")
                   .addConstraintViolation();
            valid = false;
        }

        // Validate maxLevels range (if present)
        Integer maxLevels = request.getMaxLevels();
        if (maxLevels != null && (maxLevels < MIN_LEVELS || maxLevels > MAX_LEVELS)) {
            context.buildConstraintViolationWithTemplate("Max levels must be between " + MIN_LEVELS + " and " + MAX_LEVELS)
                   .addPropertyNode("maxLevels")
                   .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}