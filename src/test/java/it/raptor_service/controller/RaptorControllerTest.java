package it.raptor_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.raptor_service.model.RaptorResult;
import it.raptor_service.service.RaptorService;
import it.raptor_service.store.JobStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RaptorController.class)
class RaptorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RaptorService raptorService;

    @MockBean
    private JobStore jobStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/api/raptor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    void processText_withValidRequest_returnsAccepted() throws Exception {
        when(raptorService.processText(any(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(new RaptorResult(Collections.emptyMap(), Collections.emptyList())));

        Map<String, Object> request = Map.of("text", "This is a test text.");

        mockMvc.perform(post("/api/raptor/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void processFile_withValidFile_returnsAccepted() throws Exception {
        when(raptorService.processText(any(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(new RaptorResult(Collections.emptyMap(), Collections.emptyList())));

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getResult_whenJobCompleted_returnsOk() throws Exception {
        String jobId = UUID.randomUUID().toString();
        RaptorResult result = new RaptorResult(Collections.emptyMap(), Collections.emptyList());
        JobStore.Job mockJob = Mockito.mock(JobStore.Job.class);
        when(jobStore.getJob(jobId)).thenReturn(mockJob);
        when(mockJob.getStatus()).thenReturn(new JobStore.JobStatus("COMPLETED", result));

        mockMvc.perform(get("/api/raptor/results/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.result").exists());
    }

    @Test
    void getResult_whenJobInProgress_returnsAccepted() throws Exception {
        String jobId = UUID.randomUUID().toString();
        JobStore.Job mockJob = Mockito.mock(JobStore.Job.class);
        when(jobStore.getJob(jobId)).thenReturn(mockJob);
        when(mockJob.getStatus()).thenReturn(new JobStore.JobStatus("IN_PROGRESS", null));

        mockMvc.perform(get("/api/raptor/results/{jobId}", jobId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getResult_whenJobFailed_returnsInternalServerError() throws Exception {
        String jobId = UUID.randomUUID().toString();
        JobStore.Job mockJob = Mockito.mock(JobStore.Job.class);
        when(jobStore.getJob(jobId)).thenReturn(mockJob);
        when(mockJob.getStatus()).thenReturn(new JobStore.JobStatus("FAILED", null));

        mockMvc.perform(get("/api/raptor/results/{jobId}", jobId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getResult_whenJobNotFound_returnsNotFound() throws Exception {
        String jobId = UUID.randomUUID().toString();
        when(jobStore.getJob(jobId)).thenReturn(null);

        mockMvc.perform(get("/api/raptor/results/{jobId}", jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void processFile_withEmptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: File cannot be empty"));
    }
}
