package it.raptor_service.validator;

import it.raptor_service.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FileValidator {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int MAX_TEXT_LENGTH = 1000000;

    public String validateAndRead(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Error: File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            throw new InvalidFileException("Error: File too large. Maximum allowed: " + MAX_FILE_SIZE_MB + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("text/")) {
            throw new InvalidFileException("Error: Only text files are supported");
        }
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new InvalidFileException("Error: File content too long. Maximum allowed: " + MAX_TEXT_LENGTH + " characters");
            }
            return text;
        } catch (IOException e) {
            throw new InvalidFileException("Error reading file: " + e.getMessage());
        }
    }
}
