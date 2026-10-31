package com.ptc.contenttree.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through the real service and file-backed repository. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class TreeApiIntegrationTest {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("content-tree.data-file", () -> dataDir.resolve("tree.json").toString());
        registry.add("content-tree.cors-allowed-origins", () -> "http://allowed.example");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullLifecycle() throws Exception {
        mockMvc.perform(get("/api/tree")).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/tree").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Root\",\"content\":\"root\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(post("/api/tree").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Child\",\"content\":\"findme\",\"parentId\":1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tree/search").param("query", "FINDME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatch").value(false))
                .andExpect(jsonPath("$.children[0].isMatch").value(true));

        mockMvc.perform(post("/api/tree/move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodeId\":1,\"newParentId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot move node to its own descendant"));

        mockMvc.perform(delete("/api/tree/1"))
                .andExpect(status().isConflict());

        String persisted = Files.readString(dataDir.resolve("tree.json"));
        assertThat(persisted).contains("\"Root\"", "\"Child\"").doesNotContain("children", "isMatch");

        mockMvc.perform(delete("/api/tree/2")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children").isEmpty());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/tree")
                        .header(HttpHeaders.ORIGIN, "http://allowed.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://allowed.example"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        mockMvc.perform(options("/api/tree")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
