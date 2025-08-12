package it.raptor_service.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TextSplitterService {

    // Hierarchical separators - from most semantic to least
    private static final List<String> SEPARATORS = Arrays.asList(
            "\n\n",    // Paragraph breaks
            "\n",      // Line breaks
            ". ",      // Sentence endings with space
            "! ",      // Exclamation with space
            "? ",      // Question with space
            "; ",      // Semicolon
            ", ",      // Comma
            " ",       // Word boundaries
            ""         // Character level (fallback)
    );

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");
    private static final double CHARS_PER_TOKEN_ESTIMATE = 4.0;
    private static final double OVERLAP_RATIO = 0.1; // 10% overlap between chunks

    /**
     * Configuration for text splitting
     */
    @Getter
    public static class SplitConfig {
        private final int chunkSize;
        private final int overlapSize;
        private final boolean preserveSentences;
        private final boolean addOverlap;

        public SplitConfig(int chunkSize) {
            this(chunkSize, (int) (chunkSize * OVERLAP_RATIO), true, true);
        }

        public SplitConfig(int chunkSize, int overlapSize, boolean preserveSentences, boolean addOverlap) {
            this.chunkSize = chunkSize;
            this.overlapSize = overlapSize;
            this.preserveSentences = preserveSentences;
            this.addOverlap = addOverlap;
        }

    }

    /**
     * Split text into chunks with default configuration
     */
    public List<String> splitText(String text, int chunkSize) {
        return splitText(text, new SplitConfig(chunkSize));
    }

    /**
     * Split text into chunks with custom configuration
     */
    public List<String> splitText(String text, SplitConfig config) {
        validateInput(text);

        if (estimateTokenCount(text) <= config.getChunkSize()) {
            return Arrays.asList(text.trim());
        }

        List<String> chunks = config.isPreserveSentences()
                ? splitPreservingSentences(text, config)
                : splitRecursively(text, config.getChunkSize(), 0);

        return config.isAddOverlap() ? addOverlapToChunks(chunks, config) : chunks;
    }

    /**
     * Split text while trying to preserve sentence boundaries
     */
    private List<String> splitPreservingSentences(String text, SplitConfig config) {
        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return splitRecursively(text, config.getChunkSize(), 0);
        }

        return groupSentencesIntoChunks(sentences, config.getChunkSize());
    }

    /**
     * Recursively split text using hierarchical separators
     */
    private List<String> splitRecursively(String text, int chunkSize, int separatorIndex) {
        List<String> result = new ArrayList<>();
        String trimmedText = text.trim();

        if (estimateTokenCount(trimmedText) <= chunkSize) {
            if (!trimmedText.isEmpty()) {
                result.add(trimmedText);
            }
            return result;
        }

        if (separatorIndex >= SEPARATORS.size()) {
            return splitByCharacterCount(trimmedText, chunkSize);
        }

        String separator = SEPARATORS.get(separatorIndex);

        // Special handling for empty separator (character-level splitting)
        if (separator.isEmpty()) {
            return splitByCharacterCount(trimmedText, chunkSize);
        }

        String[] parts = trimmedText.split(Pattern.quote(separator), -1);

        if (parts.length <= 1) {
            return splitRecursively(trimmedText, chunkSize, separatorIndex + 1);
        }

        return mergeParts(parts, separator, chunkSize, separatorIndex);
    }

    /**
     * Merge parts while respecting chunk size limits
     */
    private List<String> mergeParts(String[] parts, String separator, int chunkSize, int separatorIndex) {
        List<String> result = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            String testChunk = buildTestChunk(currentChunk.toString(), part, separator);

            if (estimateTokenCount(testChunk) <= chunkSize) {
                updateCurrentChunk(currentChunk, part, separator);
            } else {
                // Finalize current chunk if it has content
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }

                // Handle the current part
                if (estimateTokenCount(part) <= chunkSize) {
                    currentChunk.append(part);
                } else {
                    result.addAll(splitRecursively(part, chunkSize, separatorIndex + 1));
                }
            }
        }

        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString().trim());
        }

        return result;
    }

    /**
     * Build test chunk to check size
     */
    private String buildTestChunk(String current, String part, String separator) {
        if (current.isEmpty()) {
            return part;
        }
        return current + separator + part;
    }

    /**
     * Update current chunk with new part
     */
    private void updateCurrentChunk(StringBuilder currentChunk, String part, String separator) {
        if (currentChunk.length() > 0) {
            currentChunk.append(separator);
        }
        currentChunk.append(part);
    }

    /**
     * Split text by character count as fallback
     */
    private List<String> splitByCharacterCount(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        int approxCharSize = (int) (chunkSize * CHARS_PER_TOKEN_ESTIMATE);

        for (int i = 0; i < text.length(); i += approxCharSize) {
            int end = Math.min(i + approxCharSize, text.length());

            // Try to break at word boundary if possible
            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > i + approxCharSize / 2) { // Don't make chunks too small
                    end = lastSpace;
                }
            }

            String chunk = text.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
        }

        return result;
    }

    /**
     * Add overlap between chunks for better context continuity
     */
    private List<String> addOverlapToChunks(List<String> chunks, SplitConfig config) {
        if (chunks.size() <= 1 || config.getOverlapSize() <= 0) {
            return chunks;
        }

        List<String> overlappedChunks = new ArrayList<>();
        overlappedChunks.add(chunks.get(0)); // First chunk unchanged

        for (int i = 1; i < chunks.size(); i++) {
            String previousChunk = chunks.get(i - 1);
            String currentChunk = chunks.get(i);

            String overlap = extractOverlap(previousChunk, config.getOverlapSize());
            if (!overlap.isEmpty()) {
                currentChunk = overlap + " " + currentChunk;

                // Ensure the chunk with overlap doesn't exceed size limit
                if (estimateTokenCount(currentChunk) > config.getChunkSize()) {
                    // Reduce overlap if necessary
                    int maxOverlapTokens = config.getChunkSize() - estimateTokenCount(chunks.get(i));
                    if (maxOverlapTokens > 0) {
                        overlap = truncateToTokenLimit(overlap, maxOverlapTokens);
                        currentChunk = overlap + " " + chunks.get(i);
                    } else {
                        currentChunk = chunks.get(i); // No overlap possible
                    }
                }
            }

            overlappedChunks.add(currentChunk);
        }

        return overlappedChunks;
    }

    /**
     * Extract overlap text from the end of a chunk
     */
    private String extractOverlap(String text, int overlapTokens) {
        if (estimateTokenCount(text) <= overlapTokens) {
            return text;
        }

        // Try to break at sentence boundary
        String[] sentences = SENTENCE_PATTERN.split(text);
        if (sentences.length > 1) {
            StringBuilder overlap = new StringBuilder();
            for (int i = sentences.length - 1; i >= 0; i--) {
                String testOverlap = sentences[i] + (overlap.length() > 0 ? " " + overlap : "");
                if (estimateTokenCount(testOverlap) <= overlapTokens) {
                    overlap.insert(0, sentences[i] + (overlap.length() > 0 ? " " : ""));
                } else {
                    break;
                }
            }
            if (overlap.length() > 0) {
                return overlap.toString().trim();
            }
        }

        // Fallback: character-based truncation
        return truncateToTokenLimit(text, overlapTokens);
    }

    /**
     * Truncate text to fit within token limit
     */
    private String truncateToTokenLimit(String text, int tokenLimit) {
        if (estimateTokenCount(text) <= tokenLimit) {
            return text;
        }

        int approxCharLimit = (int) (tokenLimit * CHARS_PER_TOKEN_ESTIMATE);
        if (text.length() <= approxCharLimit) {
            return text;
        }

        // Try to break at word boundary
        int lastSpace = text.lastIndexOf(' ', approxCharLimit);
        int cutPoint = (lastSpace > approxCharLimit / 2) ? lastSpace : approxCharLimit;

        return text.substring(Math.max(0, text.length() - cutPoint)).trim();
    }

    /**
     * Improved token count estimation
     */
    private int estimateTokenCount(String text) {
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

    /**
     * Split text into sentences with improved regex
     */
    public List<String> splitIntoSentences(String text) {
        validateInput(text);

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

    /**
     * Group sentences into chunks with better size management
     */
    private List<String> groupSentencesIntoChunks(List<String> sentences, int chunkSize) {
        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

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
                    currentChunk = new StringBuilder();
                }

                // Handle sentence that's larger than chunk size
                if (estimateTokenCount(sentence) > chunkSize) {
                    chunks.addAll(splitRecursively(sentence, chunkSize, 0));
                } else {
                    currentChunk.append(sentence);
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * Validate input text
     */
    private void validateInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
    }

    /**
     * Get chunk statistics for debugging/monitoring
     */
    public ChunkStats getChunkStats(List<String> chunks) {
        if (chunks.isEmpty()) {
            return new ChunkStats(0, 0, 0, 0);
        }

        int totalTokens = chunks.stream().mapToInt(this::estimateTokenCount).sum();
        int minTokens = chunks.stream().mapToInt(this::estimateTokenCount).min().orElse(0);
        int maxTokens = chunks.stream().mapToInt(this::estimateTokenCount).max().orElse(0);
        double avgTokens = (double) totalTokens / chunks.size();

        return new ChunkStats(chunks.size(), avgTokens, minTokens, maxTokens);
    }

    /**
         * Statistics about chunk distribution
     */

        public record ChunkStats(int chunkCount, double averageTokens, int minTokens, int maxTokens) {
        @Override
            public String toString() {
                return String.format("ChunkStats{count=%d, avg=%.1f, min=%d, max=%d}",
                        chunkCount, averageTokens, minTokens, maxTokens);
            }
        }
}