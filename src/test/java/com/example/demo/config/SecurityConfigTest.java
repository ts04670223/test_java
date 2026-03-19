package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void cartEndpoint_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/cart/1"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    public void productsEndpoint_withoutAuth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/products"))
               .andExpect(status().isOk());
    }
}
