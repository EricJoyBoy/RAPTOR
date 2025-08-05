package it.raptor_service.service;


import it.raptor_service.model.Cluster;
import it.raptor_service.model.GlobalCluster;
import it.raptor_service.model.TextEmbedding;
import org.springframework.stereotype.Service;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class ClusteringService {

    private static final double CLUSTER_THRESHOLD = 0.1;
    private static final int MAX_CLUSTERS = 50;
    private static final Random random = new Random(224); // Fixed seed for reproducibility

    /**
     * Perform hierarchical clustering on embeddings
     */
    public List<Cluster> performClustering(List<TextEmbedding> embeddings) {
        if (embeddings.isEmpty()) {
            return Collections.emptyList();
        }

        if (embeddings.size() <= 2) {
            // Handle small datasets
            return Collections.singletonList(createSingleCluster(embeddings));
        }

        try {
            // Step 1: Global clustering using dimensionality reduction simulation
            List<GlobalCluster> globalClusters = performGlobalClustering(embeddings);

            // Step 2: Local clustering within each global cluster
            List<Cluster> allClusters = new ArrayList<>();
            int clusterIdCounter = 0;

            for (GlobalCluster globalCluster : globalClusters) {
                List<Cluster> localClusters = performLocalClustering(
                        globalCluster.getEmbeddings(), clusterIdCounter);
                allClusters.addAll(localClusters);
                clusterIdCounter += localClusters.size();
            }

            return allClusters;

        } catch (Exception e) {
            System.err.println("Clustering failed, using single cluster: " + e.getMessage());
            return Collections.singletonList(createSingleCluster(embeddings));
        }
    }

    /**
     * Perform global clustering (simulating UMAP + GMM)
     */
    private List<GlobalCluster> performGlobalClustering(List<TextEmbedding> embeddings) throws Exception {
        // Convert embeddings to Weka format
        Instances data = createWekaInstances(embeddings);

        // Use EM (Expectation-Maximization) as GMM equivalent
        EM clusterer = new EM();
        clusterer.setMaxIterations(100);
        clusterer.setSeed(224);

        // Determine optimal number of clusters
        int optimalClusters = findOptimalClusters(data, Math.min(MAX_CLUSTERS, embeddings.size()));
        clusterer.setNumClusters(optimalClusters);

        clusterer.buildClusterer(data);

        // Group embeddings by cluster
        Map<Integer, List<TextEmbedding>> clusterMap = new HashMap<>();

        for (int i = 0; i < embeddings.size(); i++) {
            Instance instance = data.instance(i);
            double[] probabilities = clusterer.distributionForInstance(instance);

            // Assign to clusters based on probability threshold
            for (int j = 0; j < probabilities.length; j++) {
                if (probabilities[j] > CLUSTER_THRESHOLD) {
                    clusterMap.computeIfAbsent(j, k -> new ArrayList<>())
                            .add(embeddings.get(i));
                }
            }
        }

        return clusterMap.entrySet().stream()
                .map(entry -> new GlobalCluster(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Perform local clustering within a global cluster
     */
    private List<Cluster> performLocalClustering(List<TextEmbedding> embeddings, int startId) {
        if (embeddings.size() <= 2) {
            return Collections.singletonList(
                    new Cluster(startId,
                            embeddings.stream().map(TextEmbedding::getText).collect(Collectors.toList()),
                            embeddings.stream().map(TextEmbedding::getId).collect(Collectors.toList()))
            );
        }

        try {
            Instances data = createWekaInstances(embeddings);
            EM clusterer = new EM();
            clusterer.setMaxIterations(50);
            clusterer.setSeed(224);

            int optimalClusters = findOptimalClusters(data, Math.min(10, embeddings.size()));
            clusterer.setNumClusters(optimalClusters);
            clusterer.buildClusterer(data);

            Map<Integer, List<TextEmbedding>> localClusterMap = new HashMap<>();

            for (int i = 0; i < embeddings.size(); i++) {
                Instance instance = data.instance(i);
                int clusterAssignment = clusterer.clusterInstance(instance);
                localClusterMap.computeIfAbsent(clusterAssignment, k -> new ArrayList<>())
                        .add(embeddings.get(i));
            }

            return localClusterMap.entrySet().stream()
                    .map(entry -> new Cluster(
                            startId + entry.getKey(),
                            entry.getValue().stream().map(TextEmbedding::getText).collect(Collectors.toList()),
                            entry.getValue().stream().map(TextEmbedding::getId).collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Local clustering failed: " + e.getMessage());
            return Collections.singletonList(
                    new Cluster(startId,
                            embeddings.stream().map(TextEmbedding::getText).collect(Collectors.toList()),
                            embeddings.stream().map(TextEmbedding::getId).collect(Collectors.toList()))
            );
        }
    }

    /**
     * Convert embeddings to Weka Instances format
     */
    private Instances createWekaInstances(List<TextEmbedding> embeddings) {
        if (embeddings.isEmpty()) {
            throw new IllegalArgumentException("Embeddings list cannot be empty");
        }

        int dimensions = embeddings.get(0).getEmbedding().length;

        // Create attributes for each dimension
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            attributes.add(new Attribute("dim_" + i));
        }

        Instances data = new Instances("embeddings", attributes, embeddings.size());

        // Add instances
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

    /**
     * Find optimal number of clusters using BIC-like approach
     */
    private int findOptimalClusters(Instances data, int maxClusters) {
        if (maxClusters <= 1) return 1;

        double bestScore = Double.POSITIVE_INFINITY;
        int bestK = 1;

        for (int k = 1; k <= Math.min(maxClusters, data.numInstances() - 1); k++) {
            try {
                EM clusterer = new EM();
                clusterer.setNumClusters(k);
                clusterer.setSeed(224);
                clusterer.buildClusterer(data);

                double logLikelihood = clusterer.getMinLogLikelihoodImprovementCV();
                int numParams = k * (data.numAttributes() + 1);
                double bic = -2 * logLikelihood + numParams * Math.log(data.numInstances());

                if (bic < bestScore) {
                    bestScore = bic;
                    bestK = k;
                }
            } catch (Exception e) {
                // Skip this k value if clustering fails
                continue;
            }
        }

        return bestK;
    }

    /**
     * Create a single cluster containing all embeddings
     */
    private Cluster createSingleCluster(List<TextEmbedding> embeddings) {
        return new Cluster(0,
                embeddings.stream().map(TextEmbedding::getText).collect(Collectors.toList()),
                embeddings.stream().map(TextEmbedding::getId).collect(Collectors.toList()));
    }
}