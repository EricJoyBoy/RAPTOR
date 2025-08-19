package it.raptor_service.service.postprocessing;

import it.raptor_service.config.RaptorProperties;
import it.raptor_service.model.Cluster;
import it.raptor_service.service.factory.ClusterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ClusterPostProcessor {

    private final RaptorProperties properties;
    private final ClusterFactory clusterFactory;

    public ClusterPostProcessor(RaptorProperties properties, ClusterFactory clusterFactory) {
        this.properties = properties;
        this.clusterFactory = clusterFactory;
    }

    public List<Cluster> postProcessClusters(List<Cluster> clusters) {
        List<Cluster> validClusters = removeEmptyClusters(clusters);

        int minClusterSize = properties.getClustering().getMinClusterSize();
        if (minClusterSize > 1) {
            validClusters = mergeSmallClusters(validClusters, minClusterSize);
        }

        log.info("Post-processing: {} -> {} clusters", clusters.size(), validClusters.size());
        return validClusters;
    }

    private List<Cluster> removeEmptyClusters(List<Cluster> clusters) {
        return clusters.stream()
                .filter(cluster -> !cluster.getTexts().isEmpty())
                .toList();
    }

    private List<Cluster> mergeSmallClusters(List<Cluster> clusters, int minSize) {
        List<Cluster> largeClusters = new ArrayList<>();
        List<Cluster> smallClusters = new ArrayList<>();

        for (Cluster cluster : clusters) {
            if (cluster.getTexts().size() >= minSize) {
                largeClusters.add(cluster);
            } else {
                smallClusters.add(cluster);
            }
        }

        if (!smallClusters.isEmpty()) {
            Cluster mergedCluster = mergeClusters(smallClusters, largeClusters.size());
            largeClusters.add(mergedCluster);
            log.debug("Merged {} small clusters into one", smallClusters.size());
        }

        return largeClusters;
    }

    private Cluster mergeClusters(List<Cluster> clusters, int newId) {
        List<String> allTexts = clusters.stream()
                .flatMap(c -> c.getTexts().stream())
                .toList();

        List<Integer> allIds = clusters.stream()
                .map(Cluster::getId)
                .toList();

        return new Cluster(newId, allTexts, allIds);
    }
}
