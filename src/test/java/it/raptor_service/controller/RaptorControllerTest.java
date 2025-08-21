package it.raptor_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.raptor_service.model.RaptorResult;
import it.raptor_service.service.RaptorService;
import it.raptor_service.service.validator.RaptorControllerValidator;
import it.raptor_service.web.rest.RaptorController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.Map;

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
    private RaptorControllerValidator raptorControllerValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/api/raptor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("RAPTOR Text Processing Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void processText_withValidRequest_returnsOk() throws Exception {
        RaptorResult mockResult = new RaptorResult(Collections.emptyMap(), Collections.emptyList());
        when(raptorService.processText(any(), anyInt(), anyInt())).thenReturn(mockResult);

        Map<String, Object> request = Map.of(
                "text", "This is a test text.",
                "chunkSize", 500,
                "maxLevels", 2
        );

        mockMvc.perform(post("/api/raptor/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void processText_withEmptyText_returnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of("text", "");

        mockMvc.perform(post("/api/raptor/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processFile_withValidFile_returnsOk() throws Exception {
        RaptorResult mockResult = new RaptorResult(Collections.emptyMap(), Collections.emptyList());
        when(raptorService.processText(any(), anyInt(), anyInt())).thenReturn(mockResult);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is the file content.".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file")
                        .file(file)
                        .param("chunkSize", "500")
                        .param("maxLevels", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void processFile_withEmptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        when(raptorControllerValidator.validateFile(any())).thenReturn("Error: File cannot be empty");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: File cannot be empty"));
    }

    @Test
    void processFile_withLargeFile_returnsBadRequest() throws Exception {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        when(raptorControllerValidator.validateFile(any())).thenReturn("Error: File too large. Maximum allowed: 10MB");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: File too large. Maximum allowed: 10MB"));
    }

    @Test
    void processFile_withInvalidFileType_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "image content".getBytes()
        );

        when(raptorControllerValidator.validateFile(any())).thenReturn("Error: Only text files are supported");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Only text files are supported"));
    }

    @Test
    void processFile_withLongContent_returnsBadRequest() throws Exception {
        // Create a string that is longer than MAX_TEXT_LENGTH
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000001; i++) {
            longText.append("a");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "long.txt",
                "text/plain",
                longText.toString().getBytes()
        );

        when(raptorControllerValidator.validateTextLength(any())).thenReturn("Error: File content too long. Maximum allowed: 1000000 characters");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/raptor/process-file")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: File content too long. Maximum allowed: 1000000 characters"));
    }
}
