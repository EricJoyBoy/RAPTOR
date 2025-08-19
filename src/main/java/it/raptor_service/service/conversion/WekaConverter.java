package it.raptor_service.service.conversion;


import it.raptor_service.model.TextEmbedding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class WekaConverter {

    private final Map<Integer, Instances> instancesCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    public Instances convertToWekaInstances(List<TextEmbedding> embeddings) {
        if (embeddings.isEmpty()) {
            throw new IllegalArgumentException("Embeddings list cannot be empty");
        }

        // Create cache key based on embeddings hash
        int cacheKey = createCacheKey(embeddings);
        if (instancesCache.containsKey(cacheKey)) {
            log.debug("Using cached Weka instances for {} embeddings", embeddings.size());
            return instancesCache.get(cacheKey);
        }

        Instances instances = createInstances(embeddings);
        cacheInstances(cacheKey, instances);

        return instances;
    }

    private Instances createInstances(List<TextEmbedding> embeddings) {
        int dimensions = embeddings.get(0).getEmbedding().length;

        ArrayList<Attribute> attributes = IntStream.range(0, dimensions)
                .mapToObj(i -> new Attribute("dim_" + i))
                .collect(Collectors.toCollection(ArrayList::new));

        Instances data = new Instances("embeddings", attributes, embeddings.size());

        for (TextEmbedding embedding : embeddings) {
            Instance instance = new DenseInstance(dimensions);
            float[] embeddingArray = embedding.getEmbedding();

            for (int i = 0; i < dimensions; i++) {
                instance.setValue(i, embeddingArray[i]);
            }
            data.add(instance);
        }

        return data;
    }

    private int createCacheKey(List<TextEmbedding> embeddings) {
        return Objects.hash(
                embeddings.size(),
                embeddings.get(0).getEmbedding().length,
                embeddings.stream().limit(10).mapToInt(Object::hashCode).sum()
        );
    }

    private void cacheInstances(int key, Instances instances) {
        if (instancesCache.size() >= MAX_CACHE_SIZE) {
            // Simple LRU: remove oldest entry
            Integer oldestKey = instancesCache.keySet().iterator().next();
            instancesCache.remove(oldestKey);
        }
        instancesCache.put(key, instances);
    }

    public void clearCache() {
        instancesCache.clear();
        log.debug("Weka instances cache cleared");
    }
}
