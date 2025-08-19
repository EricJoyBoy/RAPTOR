package it.raptor_service.service.optimization;

import it.raptor_service.config.RaptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import weka.clusterers.EM;
import weka.core.Instances;

@Slf4j
@Component
public class ClusterOptimizer {

    private final RaptorProperties properties;

    public ClusterOptimizer(RaptorProperties properties) {
        this.properties = properties;
    }

    public int findOptimalClusterCount(Instances data, int maxClusters) {
        if (maxClusters <= 1 || data.numInstances() <= 1) {
            return 1;
        }

        double bestScore = Double.POSITIVE_INFINITY;
        int bestK = 1;
        int noImprovementCount = 0;

        log.debug("Finding optimal cluster count for {} instances, max clusters: {}",
                data.numInstances(), maxClusters);

        for (int k = 1; k <= Math.min(maxClusters, data.numInstances() - 1); k++) {
            try {
                double bic = calculateBIC(data, k);

                if (bic < bestScore) {
                    bestScore = bic;
                    bestK = k;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }

                // Early stopping
                if (noImprovementCount >= 3 && k > 3) {
                    log.debug("Early stopping at k={} due to no improvement", k);
                    break;
                }

            } catch (Exception e) {
                log.debug("Failed to evaluate k={}: {}", k, e.getMessage());
            }
        }

        log.debug("Optimal cluster count: {} (BIC score: {})", bestK, bestScore);
        return bestK;
    }

    private double calculateBIC(Instances data, int k) throws Exception {
        EM clusterer = new EM();
        clusterer.setNumClusters(k);
        clusterer.setSeed(properties.getClustering().getSeed());
        clusterer.setMaxIterations(50);
        clusterer.buildClusterer(data);

        double logLikelihood = clusterer.getMinLogLikelihoodImprovementCV();
        int numParams = k * (data.numAttributes() + 1);

        return -2 * logLikelihood + numParams * Math.log(data.numInstances());
    }
}

