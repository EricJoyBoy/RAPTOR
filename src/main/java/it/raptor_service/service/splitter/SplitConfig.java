package it.raptor_service.service.splitter;

import lombok.Getter;

/**
 * Configuration for text splitting
 */
@Getter
public class SplitConfig {
    private static final double OVERLAP_RATIO = 0.1; // 10% overlap between chunks

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
