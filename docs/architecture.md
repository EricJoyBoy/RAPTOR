# RAPTOR Service Architecture

## Overview

The RAPTOR (Recursive Abstractive Processing for Text Organization and Retrieval) Service is a Spring Boot application that implements a hierarchical text processing and clustering algorithm. The service processes large text documents by breaking them into chunks, generating embeddings, clustering similar content, and creating hierarchical summaries.

## Current Architecture

### Technology Stack

- **Framework**: Spring Boot 3.2.0 with Java 21
- **AI Integration**: Spring AI 1.0.0-M6 with Ollama integration
- **Clustering**: Weka 3.8.6 for machine learning algorithms
- **Document Processing**: Apache Tika 2.9.1 for file parsing
- **Build Tool**: Maven

### Architecture Components

#### 1. Presentation Layer
```
RaptorController
├── /api/raptor/process (POST) - Process text input
├── /api/raptor/process-file (POST) - Process uploaded files
└── /api/raptor/health (GET) - Health check
```

**Responsibilities:**
- Handle HTTP requests and responses
- Validate input parameters
- Convert between DTOs and domain objects
- Provide REST API endpoints

#### 2. Service Layer
```
RaptorService (Main Orchestrator)
├── TextSplitterService - Text chunking
├── ClusteringService - Embedding clustering
└── External AI Services (Ollama)
```

**Responsibilities:**
- **RaptorService**: Orchestrates the entire RAPTOR algorithm
- **TextSplitterService**: Splits large texts into manageable chunks
- **ClusteringService**: Performs hierarchical clustering on embeddings

#### 3. Configuration Layer
```
OllamaConfiguration
├── ChatClient - For text summarization
└── EmbeddingModel - For text embeddings
```

**Responsibilities:**
- Configure AI model connections
- Manage Spring AI integration
- Handle Ollama API configuration

#### 4. Model Layer
```
Data Models
├── RaptorResult - Final processing result
├── LevelResult - Per-level processing results
├── Cluster - Clustering results
├── ClusterSummary - Generated summaries
├── TextEmbedding - Text with vector representation
└── ProcessResponse - API response wrapper
```

### Data Flow

1. **Input Processing**
   ```
   Text/File → RaptorController → RaptorService
   ```

2. **Text Chunking**
   ```
   Large Text → TextSplitterService → List<String> chunks
   ```

3. **Embedding Generation**
   ```
   Text Chunks → Ollama Embedding Model → TextEmbedding objects
   ```

4. **Clustering**
   ```
   Embeddings → ClusteringService → Clusters
   ```

5. **Summarization**
   ```
   Clusters → Ollama Chat Model → ClusterSummaries
   ```

6. **Recursive Processing**
   ```
   Summaries → Next Level → Repeat until max levels reached
   ```

### Key Algorithms

#### Text Splitting Algorithm
- **Hierarchical Splitting**: Uses multiple separators (paragraphs, sentences, words)
- **Token Estimation**: Rough approximation using character count
- **Fallback Strategy**: Character-based splitting when semantic splitting fails

#### Clustering Algorithm
- **Two-Phase Clustering**: Global clustering followed by local clustering
- **EM Algorithm**: Expectation-Maximization for Gaussian Mixture Models
- **BIC Optimization**: Bayesian Information Criterion for optimal cluster count
- **Threshold-Based Assignment**: Probability-based cluster assignment

#### RAPTOR Algorithm
- **Recursive Processing**: Multi-level hierarchical processing
- **Embedding Generation**: Vector representation of text chunks
- **Clustering**: Grouping similar content
- **Summarization**: AI-powered content summarization
- **Hierarchical Structure**: Building tree-like document organization

## Current Strengths

1. **Modular Design**: Clear separation of concerns with dedicated services
2. **Flexible AI Integration**: Spring AI abstraction allows easy model switching
3. **Robust Error Handling**: Graceful fallbacks for clustering failures
4. **Configurable Parameters**: Adjustable chunk size and max levels
5. **File Upload Support**: Handles both text input and file uploads
6. **Health Monitoring**: Built-in health check endpoint

## Identified Issues and Improvements

### 1. **Performance and Scalability**

**Current Issues:**
- Synchronous processing blocks the entire request
- No caching mechanism for repeated requests
- Memory-intensive embedding storage
- No request queuing for large files

**Improvements:**
```java
// Add async processing
@Async
public CompletableFuture<RaptorResult> processTextAsync(String text, int chunkSize, int maxLevels)

// Add caching
@Cacheable(value = "raptor-results", key = "#text.hashCode() + '_' + #chunkSize + '_' + #maxLevels")
public RaptorResult processText(String text, int chunkSize, int maxLevels)

// Add request queuing
@RabbitListener(queues = "raptor-processing-queue")
public void processQueuedRequest(ProcessRequest request)
```

### 2. **Error Handling and Resilience**

**Current Issues:**
- Limited error recovery mechanisms
- No retry logic for AI service failures
- Missing circuit breaker pattern
- Inadequate logging for debugging

**Improvements:**
```java
// Add circuit breaker
@CircuitBreaker(name = "ollama-service", fallbackMethod = "fallbackEmbedding")
public List<TextEmbedding> generateEmbeddings(List<String> texts)

// Add retry mechanism
@Retry(name = "ai-service-retry", fallbackMethod = "fallbackSummarization")
public String generateSummary(String context)

// Add comprehensive logging
@Slf4j
public class RaptorService {
    public RaptorResult processText(String text, int chunkSize, int maxLevels) {
        log.info("Processing text with chunkSize={}, maxLevels={}", chunkSize, maxLevels);
        // ... processing logic
    }
}
```

### 3. **Configuration Management**

**Current Issues:**
- Hard-coded parameters in services
- Limited environment-specific configuration
- No feature flags for algorithm variants

**Improvements:**
```java
@ConfigurationProperties(prefix = "raptor")
public class RaptorProperties {
    private int defaultChunkSize = 2000;
    private int defaultMaxLevels = 3;
    private double clusterThreshold = 0.1;
    private int maxClusters = 50;
    private boolean enableCaching = true;
    private boolean enableAsyncProcessing = false;
}
```

### 4. **Monitoring and Observability**

**Current Issues:**
- No metrics collection
- Limited tracing capabilities
- No performance monitoring
- Missing health checks for external services

**Improvements:**
```java
// Add metrics
@Timed(name = "raptor.processing.time")
@Counted(name = "raptor.requests.total")
public RaptorResult processText(String text, int chunkSize, int maxLevels)

// Add distributed tracing
@NewSpan("raptor-processing")
public RaptorResult processText(String text, int chunkSize, int maxLevels)

// Add health indicators
@Component
public class OllamaHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check Ollama service availability
    }
}
```

### 5. **Security and Validation**

**Current Issues:**
- No input validation beyond basic checks
- Missing rate limiting
- No authentication/authorization
- Potential for resource exhaustion attacks

**Improvements:**
```java
// Add input validation
@Valid
public ResponseEntity<ProcessResponse> processText(@RequestBody @Valid ProcessRequest request)

// Add rate limiting
@RateLimiter(name = "raptor-api")
public ResponseEntity<ProcessResponse> processText(@RequestBody ProcessRequest request)

// Add file size limits
@PostMapping("/process-file")
public ResponseEntity<ProcessResponse> processFile(
    @RequestParam("file") @MaxFileSize(maxSize = "10MB") MultipartFile file)
```

### 6. **Testing Strategy**

**Current Issues:**
- Limited test coverage
- No integration tests
- Missing performance tests
- No AI service mocking

**Improvements:**
```java
// Add comprehensive unit tests
@Test
void shouldProcessTextWithValidInput() {
    // Test with mocked AI services
}

// Add integration tests
@SpringBootTest
@AutoConfigureTestDatabase
class RaptorServiceIntegrationTest {
    // Test with real AI services
}

// Add performance tests
@Benchmark
public void benchmarkTextProcessing() {
    // Performance benchmarking
}
```

### 7. **Architecture Enhancements**

**Proposed New Components:**

```
Enhanced Architecture
├── API Gateway (Spring Cloud Gateway)
├── Service Discovery (Eureka)
├── Configuration Server (Spring Cloud Config)
├── Message Queue (RabbitMQ/Kafka)
├── Cache Layer (Redis)
├── Monitoring (Prometheus + Grafana)
├── Tracing (Jaeger/Zipkin)
└── Load Balancer (Nginx)
```

**New Service Structure:**
```
Enhanced Services
├── RaptorOrchestratorService - Main orchestrator
├── RaptorProcessingService - Core processing logic
├── RaptorCachingService - Result caching
├── RaptorQueueService - Request queuing
├── RaptorMonitoringService - Metrics and health
└── RaptorSecurityService - Security and validation
```

### 8. **Data Management**

**Current Issues:**
- No persistent storage for results
- No result versioning
- Missing data retention policies

**Improvements:**
```java
// Add result persistence
@Entity
public class ProcessingResult {
    @Id
    private String id;
    private String inputHash;
    private RaptorResult result;
    private LocalDateTime createdAt;
    private ProcessingStatus status;
}

// Add result versioning
@Version
private Long version;

// Add data retention
@Scheduled(fixedRate = 86400000) // Daily
public void cleanupOldResults() {
    // Remove results older than 30 days
}
```

## Implementation Priority

### Phase 1: Critical Improvements (1-2 weeks)
1. Add comprehensive error handling and logging
2. Implement input validation and security measures
3. Add health checks for external services
4. Create comprehensive test suite

### Phase 2: Performance Enhancements (2-3 weeks)
1. Implement async processing
2. Add caching layer
3. Implement request queuing
4. Add monitoring and metrics

### Phase 3: Scalability Features (3-4 weeks)
1. Implement microservices architecture
2. Add service discovery and load balancing
3. Implement distributed tracing
4. Add configuration management

### Phase 4: Advanced Features (4-6 weeks)
1. Add result persistence and versioning
2. Implement advanced clustering algorithms
3. Add support for multiple AI providers
4. Create admin dashboard

## Conclusion

The current RAPTOR service provides a solid foundation for hierarchical text processing. The modular design and Spring AI integration make it flexible and extensible. However, implementing the suggested improvements will significantly enhance its production readiness, performance, and maintainability.

The phased implementation approach ensures that critical issues are addressed first while building toward a more robust, scalable architecture that can handle enterprise-level workloads.
