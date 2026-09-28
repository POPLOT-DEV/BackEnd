package com.poplot.feature.devauth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"prod", "test"})
@SpringBootTest
@AutoConfigureMockMvc
class DevAuthProfileIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("development token endpoint is absent outside local and dev profiles")
    void endpointIsAbsentOutsideDevelopmentProfiles() throws Exception {
        mockMvc.perform(post("/dev/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
    }
}
