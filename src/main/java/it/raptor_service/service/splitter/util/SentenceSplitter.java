package it.raptor_service.service.splitter.util;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SentenceSplitter {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    /**
     * Split text into sentences with improved regex
     */
    public List<String> splitIntoSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] sentences = SENTENCE_PATTERN.split(text.trim());
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }
}
