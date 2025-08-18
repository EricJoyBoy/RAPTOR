package it.raptor_service.service.splitter.util;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    private static final double CHARS_PER_TOKEN_ESTIMATE = 4.0;

    /**
     * Improved token count estimation
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        double baseCount = normalized.length() / CHARS_PER_TOKEN_ESTIMATE;

        long punctuationCount = normalized.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();

        return Math.max(1, (int) Math.ceil(baseCount + punctuationCount * 0.3));
    }

    public int getApproxCharSize(int chunkSize) {
        return (int) (chunkSize * CHARS_PER_TOKEN_ESTIMATE);
    }
}
