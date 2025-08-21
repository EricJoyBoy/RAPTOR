
# RAPTOR Service
[![Build and Test](https://github.com/EricJoyBoy/RAPTOR/actions/workflows/build.yml/badge.svg)](https://github.com/EricJoyBoy/RAPTOR/actions/workflows/build.yml)
[![Dependabot Updates](https://github.com/EricJoyBoy/RAPTOR/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/EricJoyBoy/RAPTOR/actions/workflows/dependabot/dependabot-updates)

This project implements the RAPTOR (Recursive Abstractive Processing for Tree-Organized Retrieval) algorithm using Spring AI.

## Description

The RAPTOR algorithm is a novel approach to text summarization and retrieval that leverages a tree-like structure to represent and process information. It recursively clusters and summarizes text chunks, creating a hierarchical understanding of the document. This allows for more efficient and context-aware retrieval of information.

This service provides a RESTful API for processing text using the RAPTOR algorithm. It takes a text or a file as input and returns a `RaptorResult` object containing the clustered and summarized information at different levels of the hierarchy.

## Getting Started

### Prerequisites

*   Java 21
*   Maven

### Building and Running

1.  Clone the repository:
    ```bash
    git clone https://github.com/EricJoyBoy/raptor-service.git
    ```
2.  Navigate to the project directory:
    ```bash
    cd raptor-service
    ```
3.  Build the project using Maven:
    ```bash
    ./mvnw clean install
    ```
4.  Run the application:
    ```bash
    java -jar target/raptor-service-1.0.0.jar
    ```

## Configuration

The application can be configured by editing the `src/main/resources/application.properties` file.

### Spring AI Provider

This service uses Spring AI, and you can configure it to use different AI providers. By default, it's configured to use Ollama.

**Ollama Configuration:**
```properties
# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=mistral
spring.ai.ollama.embedding.model=mistral

# Chat options
spring.ai.ollama.chat.options.temperature=0.7
spring.ai.ollama.chat.options.top-p=0.9
spring.ai.ollama.chat.options.num-predict=2048
```

To use other providers like Azure OpenAI, you would need to add the corresponding starter to the `pom.xml` and configure the properties in `application.properties`.

## Key Features

*   **Text Processing:** The service can process raw text or text extracted from a file.
*   **Recursive Summarization:** It recursively summarizes text chunks to create a hierarchical representation.
*   **Clustering:** It uses a clustering algorithm to group similar text chunks together.
*   **Spring AI Integration:** It leverages the Spring AI library for AI-powered text processing.
*   **RESTful API:** It exposes a simple and intuitive RESTful API for easy integration.

## How to Use

To use the service, you can send a POST request to the `/api/raptor/process` or `/api/raptor/process-file` endpoint with the text or file you want to process. The service will return a JSON object containing the processed information.

### Process Text

*   **URL:** `/api/raptor/process`
*   **Method:** `POST`
*   **Description:** Processes raw text using the RAPTOR algorithm.
*   **Request Body:**

```json
{
  "text": "The text to process.",
  "chunkSize": 2000,
  "maxLevels": 3
}
```

*   **Response:**

```json
{
  "message": "Success",
  "result": {
    "results": {
      "1": {
        "level": 1,
        "embeddings": [
          {
            "id": 0,
            "text": "The first chunk of text.",
            "embedding": [ ... ]
          }
        ],
        "clusters": [
          {
            "id": 0,
            "texts": [ "The first chunk of text." ],
            "textIds": [ 0 ]
          }
        ],
        "summaries": [
          {
            "clusterId": 0,
            "level": 1,
            "summary": "A summary of the first chunk of text.",
            "textIds": [ 0 ]
          }
        ]
      }
    },
    "allTexts": [ "The first chunk of text.", "A summary of the first chunk of text." ]
  }
}
```

### Process File

*   **URL:** `/api/raptor/process-file`
*   **Method:** `POST`
*   **Description:** Processes a file using the RAPTOR algorithm.
*   **Request Parameters:**

| Name      | Type          | Description                                       |
| :-------- | :------------ | :------------------------------------------------ |
| `file`    | `MultipartFile` | The file to process.                              |
| `chunkSize` | `int`         | The size of the text chunks (default: 2000).      |
| `maxLevels` | `int`         | The maximum number of levels to process (default: 3). |

*   **Response:**

Same as the `/api/raptor/process` endpoint.

### Health Check

*   **URL:** `/api/raptor/health`
*   **Method:** `GET`
*   **Description:** Returns the health status of the service.
*   **Response:**

```json
{
  "status": "healthy",
  "service": "RAPTOR Text Processing Service"
}
```

## Dependencies

This project uses the following key dependencies:

*   **Spring Boot:** Framework for creating stand-alone, production-grade Spring based Applications.
*   **Spring AI:** An artificial intelligence-centric project for Spring developers.
*   **Weka:** A collection of machine learning algorithms for data mining tasks.
*   **Apache Commons Math:** A library of mathematics and statistics components.
*   **Apache Tika:** A toolkit for detecting and extracting metadata and text from various file types.
*   **Jackson:** A suite of data-processing tools for Java.
