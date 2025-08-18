package it.raptor_service.controller;


import it.raptor_service.model.ProcessResponse;
import it.raptor_service.model.RaptorResult;
import it.raptor_service.service.RaptorService;
import it.raptor_service.service.validator.RaptorControllerValidator;
import it.raptor_service.service.validator.ValidRaptorRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/raptor")
@CrossOrigin(origins = "*")
@Slf4j
public class RaptorController {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int MAX_TEXT_LENGTH = 1000000;

    private final RaptorService raptorService;
    private final RaptorControllerValidator raptorControllerValidator;

    public RaptorController(RaptorService raptorService, RaptorControllerValidator raptorControllerValidator) {
        this.raptorService = raptorService;
        this.raptorControllerValidator = raptorControllerValidator;
    }

    /**
     * Process text using RAPTOR algorithm
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> processText(@RequestBody @Valid ProcessRequest request) {
        log.info("Processing text request with chunkSize={}, maxLevels={}", 
                request.getChunkSize(), request.getMaxLevels());
    
        try {
            int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 2000;
            int maxLevels = request.getMaxLevels() != null ? request.getMaxLevels() : 3;

            RaptorResult result = raptorService.processText(request.getText(), chunkSize, maxLevels);
            return ResponseEntity.ok(new ProcessResponse("Success", result));
    
        } catch (Exception e) {
            log.error("Error processing text: {}", e.getMessage(), e);
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
            @RequestParam(value = "chunkSize", defaultValue = "2000") @Min(100) @Max(10000) int chunkSize,
            @RequestParam(value = "maxLevels", defaultValue = "3") @Min(1) @Max(10) int maxLevels) {

        log.info("Processing file: {} ({} bytes) with chunkSize={}, maxLevels={}", 
                file.getOriginalFilename(), file.getSize(), chunkSize, maxLevels);

        try {
            String fileValidationError = raptorControllerValidator.validateFile(file);
            if (fileValidationError != null) {
                return ResponseEntity.badRequest().body(new ProcessResponse(fileValidationError, null));
            }

            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            String textValidationError = raptorControllerValidator.validateTextLength(text);
            if (textValidationError != null) {
                return ResponseEntity.badRequest().body(new ProcessResponse(textValidationError, null));
            }

            RaptorResult result = raptorService.processText(text, chunkSize, maxLevels);
            log.info("Successfully processed file with {} levels", result.getLevelResults().size());

            return ResponseEntity.ok(new ProcessResponse("Success", result));

        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ProcessResponse("Error reading file: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage(), e);
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
                "service", "RAPTOR Text Processing Service",
                "version", "1.0.0"
        ));
    }

    @ValidRaptorRequest
    @Data
    public static class ProcessRequest {
        @NotBlank(message = "Text cannot be empty")
        private String text;

        private Integer chunkSize;

        private Integer maxLevels;
    }

}