package com.ptc.contenttree.controller;

import tools.jackson.databind.ObjectMapper;
import com.ptc.contenttree.dto.MoveNodeRequest;
import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.exception.NodeNotFoundException;
import com.ptc.contenttree.service.TreeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TreeController.class)
class TreeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreeService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnFullTree() throws Exception {
        TreeNodeResponse root = createResponse(1L, "Root", "Root content", null);
        TreeNodeResponse child = createResponse(2L, "Child", "Child content", 1L);
        root.setChildren(List.of(child));

        when(service.getFullTree()).thenReturn(root);

        mockMvc.perform(get("/api/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Root"))
                .andExpect(jsonPath("$.children[0].name").value("Child"));
    }

    @Test
    void shouldGetNodeById() throws Exception {
        TreeNodeResponse node = createResponse(1L, "Test Node", "Test content", null);
        when(service.getById(1L)).thenReturn(node);

        mockMvc.perform(get("/api/tree/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Node"))
                .andExpect(jsonPath("$.content").value("Test content"));
    }

    @Test
    void shouldReturn404WhenNodeNotFound() throws Exception {
        doThrow(new NodeNotFoundException("Node not found: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/tree/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateNode() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("New Node", "Some content", 1L);
        TreeNodeResponse response = createResponse(3L, "New Node", "Some content", 1L);

        when(service.create(any(TreeNodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("New Node"));
    }

    @Test
    void shouldReturnBadRequestForMissingName() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("", "Some content", null);

        mockMvc.perform(post("/api/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingContent() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("Test", "", null);

        mockMvc.perform(post("/api/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateNode() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("Updated", "Updated content", 1L);
        TreeNodeResponse response = createResponse(2L, "Updated", "Updated content", 1L);

        when(service.update(eq(2L), any(TreeNodeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tree/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void shouldReturnNoContentOnDelete() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/tree/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldMoveNode() throws Exception {
        MoveNodeRequest request = new MoveNodeRequest(2L, 3L);
        doNothing().when(service).moveNode(2L, 3L);

        mockMvc.perform(post("/api/tree/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).moveNode(2L, 3L);
    }

    @Test
    void shouldSearchNodes() throws Exception {
        TreeNodeResponse root = createResponse(1L, "Root", "Root content", null);
        root.setIsMatch(false);
        TreeNodeResponse child = createResponse(2L, "Documents", "Important docs", 1L);
        child.setIsMatch(true);
        root.setChildren(List.of(child));

        when(service.search("doc")).thenReturn(root);

        mockMvc.perform(get("/api/tree/search").param("query", "doc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatch").value(false))
                .andExpect(jsonPath("$.children[0].isMatch").value(true));
    }

    @Test
    void shouldReturn404ForGetNonexistentNode() throws Exception {
        when(service.getById(999L)).thenThrow(new NodeNotFoundException("Node not found: 999"));

        mockMvc.perform(get("/api/tree/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidMove() throws Exception {
        MoveNodeRequest request = new MoveNodeRequest(1L, 2L);
        doThrow(new IllegalArgumentException("Cannot move node to its own descendant"))
                .when(service).moveNode(1L, 2L);

        mockMvc.perform(post("/api/tree/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private TreeNodeResponse createResponse(Long id, String name, String content, Long parentId) {
        TreeNodeResponse response = new TreeNodeResponse();
        response.setId(id);
        response.setName(name);
        response.setContent(content);
        response.setParentId(parentId);
        response.setChildren(new ArrayList<>());
        return response;
    }
}

