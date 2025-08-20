# RAPTOR Service Architecture

## Overview

The RAPTOR (Recursive Abstractive Processing for Text Organization and Retrieval) Service is a Spring Boot application that implements a hierarchical text processing and clustering algorithm. The service processes large text documents by breaking them into chunks, generating embeddings, clustering similar content, and creating hierarchical summaries.

## Current Architecture

### Technology Stack

- **Framework**: Spring Boot 3.2.0 with Java 21
- **AI Integration**: Spring AI `1.0.0-M6` (with some modules on `M5`, `M7` and `1.0.1`) with Ollama integration
- **Clustering**: Weka 3.8.6 for machine learning algorithms
- **Document Processing**: Apache Tika 2.9.1 for file parsing
- **Build Tool**: Maven

### Architecture Components

#### 1. Presentation Layer (web)
- **`RaptorController`**: Exposes REST endpoints for processing text and files.
  - `POST /api/raptor/process`: Processes raw text input.
  - `POST /api/raptor/process-file`: Processes uploaded files.
  - `GET /api/raptor/health`: Provides a simple health check.

#### 2. Service Layer (service)
- **`RaptorService`**: The main orchestrator that implements the core RAPTOR algorithm.
- **`TextSplitterService`**: Handles the logic for splitting large texts into smaller, manageable chunks.
- **`ClusteringService`**: Performs hierarchical clustering on text embeddings using the Weka library.

#### 3. Configuration Management (config)
- **`RaptorProperties`**: A `@ConfigurationProperties` class that centralizes all application settings, such as default chunk size, max processing levels, and feature flags for proposed features (e.g., caching, async processing).
- **AI Configuration**: Beans for `ChatModel` and `EmbeddingModel` are configured to connect to the Ollama service.

#### 4. Data Models (model)
- **`RaptorResult`**: The final result object containing the entire hierarchical structure.
- **`LevelResult`**: Contains the results for a single level of the hierarchy, including embeddings and clusters.
- **`Cluster` & `ClusterSummary`**: Represent groups of similar text and their generated summaries.
- **`TextEmbedding`**: A data class holding the text and its corresponding vector embedding.

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

### Strengths

1. **Modular Design**: Clear separation of concerns with dedicated services for text splitting, clustering, and orchestration.
2. **Flexible AI Integration**: Spring AI abstraction allows for easy switching between different AI models and providers.
3. **Configuration Driven**: Utilizes a `RaptorProperties` class for centralized management of application settings and feature flags.
4. **Basic Error Handling**: Includes graceful fallbacks for clustering and summarization failures at each level of processing.
5. **File Upload Support**: Handles both raw text input and file uploads (`.txt`, `.pdf`, etc.) through Apache Tika.
6. **Health Monitoring**: Provides a basic health check endpoint (`/api/raptor/health`).

### Limitations

1. **Synchronous Processing**: All API calls are blocking, which can lead to long wait times for large documents.
2. **No Persistence**: Processing results are not stored and are lost after the request is complete.
3. **Stateless**: The service does not maintain any state between requests.
4. **Basic Security**: Lacks advanced security features like rate limiting or authentication.
5. **Limited Observability**: While logging is implemented, there are no metrics or distributed tracing capabilities.

## Proposed Architectural Improvements

This section outlines a roadmap for enhancing the RAPTOR service from its current state into a more robust, scalable, and production-ready application. The following are proposed improvements, many of which already have feature flags in the `RaptorProperties` file.

### 1. **Performance and Scalability**

- **Asynchronous Processing**: Implement non-blocking processing to handle long-running tasks without blocking API requests.
  ```java
  @Async
  public CompletableFuture<RaptorResult> processTextAsync(String text, int chunkSize, int maxLevels)
  ```
- **Request Queuing**: Introduce a message queue (e.g., RabbitMQ or Kafka) to manage incoming requests and prevent server overload.
  ```java
  @RabbitListener(queues = "raptor-processing-queue")
  public void processQueuedRequest(ProcessRequest request)
  ```
- **Caching**: Implement a caching layer (e.g., Redis or Caffeine) to store and retrieve results for repeated requests, reducing redundant processing.
  ```java
  @Cacheable(value = "raptor-results", key = "#text.hashCode() + '_' + #chunkSize + '_' + #maxLevels")
  public RaptorResult processText(String text, int chunkSize, int maxLevels)
  ```

### 2. **Error Handling and Resilience**

- **Circuit Breaker**: Integrate a circuit breaker pattern (e.g., Resilience4j) to prevent cascading failures when external AI services are unavailable.
  ```java
  @CircuitBreaker(name = "ollama-service", fallbackMethod = "fallbackEmbedding")
  public List<TextEmbedding> generateEmbeddings(List<String> texts)
  ```
- **Retry Mechanism**: Add automatic retries for failed requests to AI services to handle transient network issues.
  ```java
  @Retry(name = "ai-service-retry", fallbackMethod = "fallbackSummarization")
  public String generateSummary(String context)
  ```
- **Enhanced Logging**: Implement structured logging to provide more detailed and searchable logs for debugging.

### 3. **Monitoring and Observability**

- **Metrics Collection**: Integrate Micrometer and Prometheus to collect application metrics (e.g., request latency, error rates, cluster counts).
  ```java
  @Timed(name = "raptor.processing.time")
  @Counted(name = "raptor.requests.total")
  public RaptorResult processText(String text, int chunkSize, int maxLevels)
  ```
- **Distributed Tracing**: Add support for distributed tracing (e.g., Jaeger or Zipkin) to trace requests across different components.
  ```java
  @NewSpan("raptor-processing")
  public RaptorResult processText(String text, int chunkSize, int maxLevels)
  ```
- **External Service Health Checks**: Implement detailed health indicators for external dependencies like the Ollama service.
  ```java
  @Component
  public class OllamaHealthIndicator implements HealthIndicator {
      @Override
      public Health health() {
          // Check Ollama service availability
      }
  }
  ```

### 4. **Security and Validation**

- **Input Validation**: Enhance input validation using `@Valid` to protect against invalid or malicious payloads.
  ```java
  @Valid
  public ResponseEntity<ProcessResponse> processText(@RequestBody @Valid ProcessRequest request)
  ```
- **Rate Limiting**: Implement rate limiting to prevent abuse and ensure fair usage.
  ```java
  @RateLimiter(name = "raptor-api")
  public ResponseEntity<ProcessResponse> processText(@RequestBody ProcessRequest request)
  ```
- **File Size Limits**: Enforce strict file size limits to prevent resource exhaustion attacks.
  ```java
  @PostMapping("/process-file")
  public ResponseEntity<ProcessResponse> processFile(
      @RequestParam("file") @MaxFileSize(maxSize = "10MB") MultipartFile file)
  ```

### 5. **Data Management**

- **Result Persistence**: Add a database (e.g., PostgreSQL or MongoDB) to store processing results, allowing users to retrieve them later.
  ```java
  @Entity
  public class ProcessingResult {
      @Id
      private String id;
      private String inputHash;
      private RaptorResult result;
      private LocalDateTime createdAt;
      private ProcessingStatus status;
  }
  ```
- **Data Versioning**: Implement result versioning to track changes if reprocessing occurs.
  ```java
  @Version
  private Long version;
  ```
- **Data Retention Policies**: Create a scheduled job to clean up old results and manage data lifecycle.
  ```java
  @Scheduled(fixedRate = 86400000) // Daily
  public void cleanupOldResults() {
      // Remove results older than 30 days
  }
  ```

### 6. **Testing Strategy**

- **Integration Tests**: Develop a comprehensive suite of integration tests that cover the entire processing pipeline.
  ```java
  @SpringBootTest
  @AutoConfigureTestDatabase
  class RaptorServiceIntegrationTest {
      // Test with real or mocked AI services
  }
  ```
- **Performance Tests**: Create performance benchmarks to measure and track the performance of the service over time.
  ```java
  @Benchmark
  public void benchmarkTextProcessing() {
      // Performance benchmarking
  }
  ```
- **AI Service Mocking**: Utilize `spring-ai-test` to mock AI service responses and ensure predictable test outcomes.

### 7. **Microservices Architecture**

For large-scale deployments, the monolithic service can be decomposed into a set of specialized microservices:

- **API Gateway (Spring Cloud Gateway)**: A single entry point for all client requests.
- **Service Discovery (Eureka/Consul)**: Allows services to find and communicate with each other dynamically.
- **Configuration Server (Spring Cloud Config)**: Centralizes configuration management for all services.
- **Raptor Processing Service**: The core service responsible for the RAPTOR algorithm.
- **Raptor Queue Service**: Manages the request queue and worker nodes.
- **Raptor Cache Service**: A dedicated service for caching results.

## Implementation Roadmap

A phased approach is recommended to implement these improvements.

### Phase 1: Foundational Improvements
1.  **Enhanced Error Handling & Logging**: Implement circuit breakers, retries, and structured logging.
2.  **Comprehensive Testing**: Build a robust suite of unit, integration, and performance tests.
3.  **Security Hardening**: Add input validation, rate limiting, and file size limits.
4.  **Detailed Health Checks**: Implement health checks for all external dependencies.

### Phase 2: Performance Enhancements
1.  **Asynchronous Processing**: Introduce async endpoints for long-running jobs.
2.  **Caching Layer**: Implement a caching solution to reduce redundant processing.
3.  **Request Queuing**: Add a message queue to manage workload and improve reliability.

### Phase 3: Scalability & Enterprise Features
1.  **Result Persistence**: Implement a database to store and manage processing results.
2.  **Observability**: Add metrics and distributed tracing.
3.  **Microservices Decomposition**: If required, begin breaking the monolith into smaller, specialized services.

## Conclusion

The RAPTOR service is currently a functional, single-purpose application that effectively implements the core RAPTOR algorithm. The existing design is modular and flexible, providing a solid foundation for future development.

By following the proposed roadmap, the service can be evolved into a production-grade, scalable, and resilient application capable of handling enterprise-level workloads. The phased approach ensures that foundational improvements are prioritized, delivering value incrementally while managing complexity.
