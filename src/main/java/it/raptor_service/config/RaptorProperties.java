package it.raptor_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "raptor")
@Data
public class RaptorProperties {
    
    private Processing processing = new Processing();
    private Clustering clustering = new Clustering();
    private Security security = new Security();
    private Monitoring monitoring = new Monitoring();
    
    @Data
    public static class Processing {
        private int defaultChunkSize = 2000;
        private int defaultMaxLevels = 3;
        private int maxTextLength = 1000000;
        private int maxFileSizeMb = 10;
        private boolean enableAsyncProcessing = false;
        private boolean enableCaching = true;
    }
    
    @Data
    public static class Clustering {
        private double clusterThreshold = 0.1;
        private int minClusterSize = 3;
        private int maxClusters = 50;
        private int maxIterations = 100;
        private int localMaxIterations = 50;
        private int seed = 224;
    }
    
    @Data
    public static class Security {
        private boolean enableRateLimiting = false;
        private int maxRequestsPerMinute = 100;
        private boolean enableAuthentication = false;
    }
    
    @Data
    public static class Monitoring {
        private boolean enableMetrics = true;
        private boolean enableTracing = false;
        private boolean enableHealthChecks = true;
    }
}
