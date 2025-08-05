package it.raptor_service.controller;


import it.raptor_service.model.ProcessResponse;
import it.raptor_service.model.RaptorResult;
import it.raptor_service.service.RaptorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/raptor")
@CrossOrigin(origins = "*")
public class RaptorController {

    private final RaptorService raptorService;

    public RaptorController(RaptorService raptorService) {
        this.raptorService = raptorService;
    }

    /**
     * Process text using RAPTOR algorithm
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> processText(@RequestBody ProcessRequest request) {
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ProcessResponse("Error: Text cannot be empty", null));
            }

            int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 2000;
            int maxLevels = request.getMaxLevels() != null ? request.getMaxLevels() : 3;

            RaptorResult result = raptorService.processText(request.getText(), chunkSize, maxLevels);

            return ResponseEntity.ok(new ProcessResponse("Success", result));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ProcessResponse("Error processing text: " + e.getMessage(), null));
        }
    }

    /**
     * Process uploaded file using RAPTOR algorithm
     */
    @PostMapping("/process-file")
    public ResponseEntity<ProcessResponse> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "2000") int chunkSize,
            @RequestParam(value = "maxLevels", defaultValue = "3") int maxLevels) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ProcessResponse("Error: File cannot be empty", null));
            }

            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            RaptorResult result = raptorService.processText(text, chunkSize, maxLevels);

            return ResponseEntity.ok(new ProcessResponse("Success", result));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(new ProcessResponse("Error reading file: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ProcessResponse("Error processing file: " + e.getMessage(), null));
        }
    }

    /**
     * Get health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "RAPTOR Text Processing Service"
        ));
    }

    // Request/Response DTOs
    public static class ProcessRequest {
        private String text;
        private Integer chunkSize;
        private Integer maxLevels;

        // Constructors
        public ProcessRequest() {
        }

        public ProcessRequest(String text, Integer chunkSize, Integer maxLevels) {
            this.text = text;
            this.chunkSize = chunkSize;
            this.maxLevels = maxLevels;
        }

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Integer getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
        }

        public Integer getMaxLevels() {
            return maxLevels;
        }

        public void setMaxLevels(Integer maxLevels) {
            this.maxLevels = maxLevels;
        }
    }

}