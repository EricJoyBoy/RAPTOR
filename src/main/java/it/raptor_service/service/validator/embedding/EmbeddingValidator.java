package it.raptor_service.service.validator.embedding;

import it.raptor_service.model.TextEmbedding;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class EmbeddingValidator {

    public void validateClusteringInput(List<TextEmbedding> embeddings) {
        if (embeddings == null) {
            throw new IllegalArgumentException("Embeddings list cannot be null");
        }

        if (embeddings.isEmpty()) {
            return; // Valid empty list
        }

        validateEmbeddingConsistency(embeddings);
    }

    public void validateEmbeddingConsistency(List<TextEmbedding> embeddings) {
        if (embeddings.isEmpty()) return;

        int expectedDimension = embeddings.get(0).getEmbedding().length;

        for (int i = 0; i < embeddings.size(); i++) {
            TextEmbedding embedding = embeddings.get(i);

            if (embedding == null) {
                throw new IllegalArgumentException("Embedding at index " + i + " cannot be null");
            }

            if (embedding.getEmbedding() == null) {
                throw new IllegalArgumentException("Embedding array at index " + i + " cannot be null");
            }

            if (embedding.getEmbedding().length != expectedDimension) {
                throw new IllegalArgumentException(
                        String.format("Inconsistent embedding dimensions: expected %d, got %d at index %d",
                                expectedDimension, embedding.getEmbedding().length, i)
                );
            }
        }
    }
}
