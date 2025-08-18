package it.raptor_service.service.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RaptorControllerValidator {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int MAX_TEXT_LENGTH = 1000000;

    public String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Error: File cannot be empty";
        }

        if (file.getSize() > MAX_FILE_SIZE_MB * 1024L * 1024L) {
            return "Error: File too large. Maximum allowed: " + MAX_FILE_SIZE_MB + "MB";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("text/")) {
            return "Error: Only text files are supported";
        }

        return null;
    }

    public String validateTextLength(String text) {
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            return "Error: File content too long. Maximum allowed: " + MAX_TEXT_LENGTH + " characters";
        }

        return null;
    }
}


