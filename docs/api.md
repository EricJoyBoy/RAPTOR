
# RAPTOR Service API

This document describes the RESTful API for the RAPTOR Service.

## Endpoints

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
