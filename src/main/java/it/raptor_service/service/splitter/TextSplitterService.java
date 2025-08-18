package it.raptor_service.service.splitter;

import it.raptor_service.service.splitter.util.SentenceSplitter;
import it.raptor_service.service.splitter.util.TokenEstimator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class TextSplitterService {

    private final TokenEstimator tokenEstimator;
    private final SentenceSplitter sentenceSplitter;

    private static final List<String> SEPARATORS = Arrays.asList(
            "\n\n", "\n", ". ", "! ", "? ", "; ", ", ", " ", ""
    );

    public List<String> splitText(String text, int chunkSize) {
        return splitText(text, new SplitConfig(chunkSize));
    }

    public List<String> splitText(String text, SplitConfig config) {
        validateInput(text);

        if (tokenEstimator.estimateTokenCount(text) <= config.getChunkSize()) {
            return Arrays.asList(text.trim());
        }

        List<String> chunks = config.isPreserveSentences()
                ? splitPreservingSentences(text, config)
                : splitRecursively(text, config.getChunkSize(), 0);

        return config.isAddOverlap() ? addOverlapToChunks(chunks, config) : chunks;
    }

    private List<String> splitPreservingSentences(String text, SplitConfig config) {
        List<String> sentences = sentenceSplitter.splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return splitRecursively(text, config.getChunkSize(), 0);
        }
        return groupSentencesIntoChunks(sentences, config.getChunkSize());
    }

    private List<String> groupSentencesIntoChunks(List<String> sentences, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (tokenEstimator.estimateTokenCount(sentence) > chunkSize) {
                chunks.addAll(splitRecursively(sentence, chunkSize, 0));
                continue;
            }
            if (currentChunk.length() > 0 && tokenEstimator.estimateTokenCount(currentChunk + " " + sentence) > chunkSize) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        return chunks;
    }

    private List<String> splitRecursively(String text, int chunkSize, int separatorIndex) {
        if (separatorIndex >= SEPARATORS.size()) {
            return splitByCharacterCount(text, chunkSize);
        }

        String separator = SEPARATORS.get(separatorIndex);
        String[] parts = text.split(Pattern.quote(separator), -1);
        List<String> result = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String part : parts) {
            if (tokenEstimator.estimateTokenCount(part) > chunkSize) {
                result.addAll(splitRecursively(part, chunkSize, separatorIndex + 1));
                continue;
            }
            if (currentChunk.length() > 0 && tokenEstimator.estimateTokenCount(currentChunk + separator + part) > chunkSize) {
                result.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            if (currentChunk.length() > 0) {
                currentChunk.append(separator);
            }
            currentChunk.append(part);
        }

        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString());
        }
        return result;
    }

    private List<String> splitByCharacterCount(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        int approxCharSize = tokenEstimator.getApproxCharSize(chunkSize);
        for (int i = 0; i < text.length(); i += approxCharSize) {
            result.add(text.substring(i, Math.min(i + approxCharSize, text.length())));
        }
        return result;
    }

    private List<String> addOverlapToChunks(List<String> chunks, SplitConfig config) {
        if (chunks.size() <= 1) return chunks;

        List<String> overlappedChunks = new ArrayList<>();
        overlappedChunks.add(chunks.get(0));

        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String curr = chunks.get(i);
            String overlap = extractOverlap(prev, config.getOverlapSize());
            overlappedChunks.add(overlap + " " + curr);
        }
        return overlappedChunks;
    }

    private String extractOverlap(String text, int overlapTokens) {
        if (tokenEstimator.estimateTokenCount(text) <= overlapTokens) {
            return text;
        }
        List<String> sentences = sentenceSplitter.splitIntoSentences(text);
        if (sentences.size() > 1) {
            StringBuilder overlap = new StringBuilder();
            for (int i = sentences.size() - 1; i >= 0; i--) {
                if (tokenEstimator.estimateTokenCount(overlap + sentences.get(i)) <= overlapTokens) {
                    overlap.insert(0, sentences.get(i) + " ");
                } else {
                    break;
                }
            }
            if (overlap.length() > 0) {
                return overlap.toString().trim();
            }
        }
        return truncateToTokenLimit(text, overlapTokens);
    }

    private String truncateToTokenLimit(String text, int tokenLimit) {
        if (tokenEstimator.estimateTokenCount(text) <= tokenLimit) {
            return text;
        }
        int approxCharLimit = tokenEstimator.getApproxCharSize(tokenLimit);
        if (text.length() <= approxCharLimit) return text;
        return text.substring(text.length() - approxCharLimit);
    }

    private void validateInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
    }

    public ChunkStats getChunkStats(List<String> chunks) {
        if (chunks.isEmpty()) {
            return new ChunkStats(0, 0, 0, 0);
        }
        int totalTokens = chunks.stream().mapToInt(tokenEstimator::estimateTokenCount).sum();
        return new ChunkStats(
                chunks.size(),
                (double) totalTokens / chunks.size(),
                chunks.stream().mapToInt(tokenEstimator::estimateTokenCount).min().orElse(0),
                chunks.stream().mapToInt(tokenEstimator::estimateTokenCount).max().orElse(0)
        );
    }
}