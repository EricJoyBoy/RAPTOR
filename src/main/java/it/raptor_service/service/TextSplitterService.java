package it.raptor_service.service;



import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TextSplitterService {

    private static final String[] SEPARATORS = {"\n\n", "\n", " ", ""};
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s*");

    /**
     * Split text into chunks of approximately the specified size
     */
    public List<String> splitText(String text, int chunkSize) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        return recursiveSplit(text, chunkSize, 0);
    }

    /**
     * Recursively split text using different separators
     */
    private List<String> recursiveSplit(String text, int chunkSize, int separatorIndex) {
        List<String> result = new ArrayList<>();

        if (text.length() <= chunkSize) {
            if (!text.trim().isEmpty()) {
                result.add(text.trim());
            }
            return result;
        }

        if (separatorIndex >= SEPARATORS.length) {
            // If we've exhausted all separators, split by character count
            return splitByCharacterCount(text, chunkSize);
        }

        String separator = SEPARATORS[separatorIndex];
        String[] parts = text.split(Pattern.quote(separator), -1);

        if (parts.length == 1) {
            // Current separator didn't split the text, try next separator
            return recursiveSplit(text, chunkSize, separatorIndex + 1);
        }

        StringBuilder currentChunk = new StringBuilder();

        for (String part : parts) {
            String testChunk = currentChunk.length() == 0 ?
                    part : currentChunk + separator + part;

            if (estimateTokenCount(testChunk) <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(separator);
                }
                currentChunk.append(part);
            } else {
                // Current chunk is full
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                // Handle the current part
                if (estimateTokenCount(part) <= chunkSize) {
                    currentChunk.append(part);
                } else {
                    // Part is too large, split it further
                    result.addAll(recursiveSplit(part, chunkSize, separatorIndex + 1));
                }
            }
        }

        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString().trim());
        }

        return result;
    }

    /**
     * Split text by character count when other methods fail
     */
    private List<String> splitByCharacterCount(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        int approxCharSize = chunkSize * 4; // Rough estimate: 1 token ≈ 4 characters

        for (int i = 0; i < text.length(); i += approxCharSize) {
            int end = Math.min(i + approxCharSize, text.length());
            String chunk = text.substring(i, end).trim();

            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
        }

        return result;
    }

    /**
     * Estimate token count (rough approximation)
     * In a real implementation, you might use a proper tokenizer
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // Simple heuristic: average 4 characters per token
        // This is a rough approximation - for production use a proper tokenizer
        return Math.max(1, text.length() / 4);
    }

    /**
     * Split text into sentences (alternative approach)
     */
    public List<String> splitIntoSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] sentences = SENTENCE_PATTERN.split(text);
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * Group sentences into chunks of approximately the specified size
     */
    public List<String> groupSentencesIntoChunks(String text, int chunkSize) {
        List<String> sentences = splitIntoSentences(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            String testChunk = currentChunk.length() == 0 ?
                    sentence : currentChunk + " " + sentence;

            if (estimateTokenCount(testChunk) <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder(sentence);
                } else {
                    // Single sentence is larger than chunk size
                    chunks.add(sentence);
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}