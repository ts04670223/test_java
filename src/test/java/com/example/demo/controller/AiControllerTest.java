package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.service.JwtService;
import com.example.demo.service.LlmService;
import com.example.demo.service.ProductService;

@WebMvcTest(controllers = AiController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmService llmService;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @Test
    public void healthEndpoint_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/ai/health"))
               .andExpect(status().isOk());
    }

    @Test
    public void chatEndpoint_withValidRequest_shouldReturn200() throws Exception {
        when(llmService.chat(anyString(), anyList(), isNull())).thenReturn("這是 AI 的回應");

        String body = "{\"message\": \"你好\"}";
        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
               .andExpect(status().isOk());
    }
}
