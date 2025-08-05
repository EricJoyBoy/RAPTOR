
# RAPTOR Service

This project implements the RAPTOR (Recursive Abstractive Processing for Tree-Organized Retrieval) algorithm using Spring AI.

## Description

The RAPTOR algorithm is a novel approach to text summarization and retrieval that leverages a tree-like structure to represent and process information. It recursively clusters and summarizes text chunks, creating a hierarchical understanding of the document. This allows for more efficient and context-aware retrieval of information.

This service provides a RESTful API for processing text using the RAPTOR algorithm. It takes a text or a file as input and returns a `RaptorResult` object containing the clustered and summarized information at different levels of the hierarchy.

## Key Features

*   **Text Processing:** The service can process raw text or text extracted from a file.
*   **Recursive Summarization:** It recursively summarizes text chunks to create a hierarchical representation.
*   **Clustering:** It uses a clustering algorithm to group similar text chunks together.
*   **Spring AI Integration:** It leverages the Spring AI library for AI-powered text processing.
*   **RESTful API:** It exposes a simple and intuitive RESTful API for easy integration.

## How to Use

To use the service, you can send a POST request to the `/api/raptor/process` or `/api/raptor/process-file` endpoint with the text or file you want to process. The service will return a JSON object containing the processed information.

For more details on the API, please refer to the `api.md` file.
