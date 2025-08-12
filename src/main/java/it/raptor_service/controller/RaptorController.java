package it.raptor_service.controller;


import it.raptor_service.model.JobSubmissionResponse;
import it.raptor_service.model.ProcessResponse;
import it.raptor_service.model.RaptorResult;
import it.raptor_service.service.RaptorService;
import it.raptor_service.store.JobStore;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/raptor")
@CrossOrigin(origins = "*")
@Slf4j
public class RaptorController {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int MAX_TEXT_LENGTH = 1000000;

    private final RaptorService raptorService;
    private final JobStore jobStore;

    public RaptorController(RaptorService raptorService, JobStore jobStore) {
        this.raptorService = raptorService;
        this.jobStore = jobStore;
    }

    @PostMapping("/process")
    public ResponseEntity<JobSubmissionResponse> processText(@RequestBody @Valid ProcessRequest request) {
        String jobId = UUID.randomUUID().toString();
        log.info("Submitting job {} to process text with chunkSize={}, maxLevels={}",
                jobId, request.getChunkSize(), request.getMaxLevels());

        try {
            int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 2000;
            int maxLevels = request.getMaxLevels() != null ? request.getMaxLevels() : 3;

            CompletableFuture<RaptorResult> future = raptorService.processText(request.getText(), chunkSize, maxLevels);
            jobStore.storeJob(jobId, future);

            URI location = URI.create("/api/raptor/results/" + jobId);
            return ResponseEntity.accepted().location(location).body(new JobSubmissionResponse(jobId, "IN_PROGRESS", "Job submitted successfully."));

        } catch (Exception e) {
            log.error("Error submitting job to process text: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new JobSubmissionResponse(null, "FAILED", "Error submitting job: " + e.getMessage()));
        }
    }
    
    @PostMapping("/process-file")
    public ResponseEntity<JobSubmissionResponse> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "2000") @Min(100) @Max(10000) int chunkSize,
            @RequestParam(value = "maxLevels", defaultValue = "3") @Min(1) @Max(10) int maxLevels) {

        String jobId = UUID.randomUUID().toString();
        log.info("Submitting job {} to process file: {} ({} bytes) with chunkSize={}, maxLevels={}",
                jobId, file.getOriginalFilename(), file.getSize(), chunkSize, maxLevels);

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new JobSubmissionResponse(null, "FAILED", "Error: File cannot be empty"));
            }
            if (file.getSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new JobSubmissionResponse(null, "FAILED", "Error: File too large. Maximum allowed: " + MAX_FILE_SIZE_MB + "MB"));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("text/")) {
                 return ResponseEntity.badRequest().body(new JobSubmissionResponse(null, "FAILED", "Error: Only text files are supported"));
            }
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                return ResponseEntity.badRequest().body(new JobSubmissionResponse(null, "FAILED", "Error: File content too long. Maximum allowed: " + MAX_TEXT_LENGTH + " characters"));
            }

            CompletableFuture<RaptorResult> future = raptorService.processText(text, chunkSize, maxLevels);
            jobStore.storeJob(jobId, future);

            URI location = URI.create("/api/raptor/results/" + jobId);
            return ResponseEntity.accepted().location(location).body(new JobSubmissionResponse(jobId, "IN_PROGRESS", "Job submitted successfully."));

        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new JobSubmissionResponse(null, "FAILED", "Error reading file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new JobSubmissionResponse(null, "FAILED", "Error processing file: " + e.getMessage()));
        }
    }

    @GetMapping("/results/{jobId}")
    public ResponseEntity<?> getResult(@PathVariable String jobId) {
        log.debug("Checking result for job {}", jobId);
        JobStore.Job job = jobStore.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        JobStore.JobStatus status = job.getStatus();
        switch (status.status()) {
            case "COMPLETED":
                log.info("Job {} completed successfully.", jobId);
                return ResponseEntity.ok(new ProcessResponse("Success", status.result()));
            case "IN_PROGRESS":
                log.debug("Job {} is still in progress.", jobId);
                URI location = URI.create("/api/raptor/results/" + jobId);
                return ResponseEntity.accepted().location(location).body(Map.of("status", "IN_PROGRESS"));
            case "FAILED":
                log.error("Job {} failed to process.", jobId);
                return ResponseEntity.status(500).body(Map.of("status", "FAILED", "message", "Job failed to process. Check logs for details."));
            default:
                log.warn("Job {} has an unknown status.", jobId);
                return ResponseEntity.status(500).body(Map.of("status", "UNKNOWN"));
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

    @Data
    public static class ProcessRequest {
        @NotBlank(message = "Text cannot be empty")
        private String text;

        private Integer chunkSize;

        private Integer maxLevels;
    }

}