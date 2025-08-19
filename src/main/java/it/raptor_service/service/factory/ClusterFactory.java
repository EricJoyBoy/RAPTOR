package it.raptor_service.service.factory;

import it.raptor_service.model.Cluster;
import it.raptor_service.model.GlobalCluster;
import it.raptor_service.model.TextEmbedding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClusterFactory {

    public Cluster createSingleCluster(List<TextEmbedding> embeddings) {
        return createCluster(0, embeddings);
    }

    public Cluster createCluster(int id, List<TextEmbedding> embeddings) {
        List<String> texts = embeddings.stream()
                .map(TextEmbedding::getText)
                .collect(Collectors.toList());

        List<Integer> ids = embeddings.stream()
                .map(TextEmbedding::getId)
                .collect(Collectors.toList());

        return new Cluster(id, texts, ids);
    }

    public List<Cluster> createTwoSeparateClusters(List<TextEmbedding> embeddings) {
        if (embeddings.size() != 2) {
            throw new IllegalArgumentException("Expected exactly 2 embeddings");
        }

        return List.of(
                createCluster(0, Collections.singletonList(embeddings.get(0))),
                createCluster(1, Collections.singletonList(embeddings.get(1)))
        );
    }

    public GlobalCluster createGlobalCluster(int id, List<TextEmbedding> embeddings) {
        return new GlobalCluster(id, embeddings);
    }
}