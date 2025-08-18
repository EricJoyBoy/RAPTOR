package it.raptor_service.service.splitter;

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
