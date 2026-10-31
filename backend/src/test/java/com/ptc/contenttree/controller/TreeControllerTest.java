package com.ptc.contenttree.controller;

import tools.jackson.databind.ObjectMapper;
import com.ptc.contenttree.dto.MoveNodeRequest;
import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.exception.InvalidTreeOperationException;
import com.ptc.contenttree.exception.NodeNotFoundException;
import com.ptc.contenttree.exception.RootNodeDeletionException;
import com.ptc.contenttree.service.TreeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

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
        TreeNodeResponse child = createResponse(2L, "Child", "Child content", 1L);
        TreeNodeResponse root = new TreeNodeResponse(1L, "Root", "Root content", null, List.of(child), null);

        when(service.getFullTree()).thenReturn(Optional.of(root));

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
        TreeNodeResponse child = new TreeNodeResponse(2L, "Documents", "Important docs", 1L, List.of(), true);
        TreeNodeResponse root = new TreeNodeResponse(1L, "Root", "Root content", null, List.of(child), false);

        when(service.search("doc")).thenReturn(Optional.of(root));

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
        doThrow(new InvalidTreeOperationException("Cannot move node to its own descendant"))
                .when(service).moveNode(1L, 2L);

        mockMvc.perform(post("/api/tree/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot move node to its own descendant"));
    }

    @Test
    void shouldReturnNoContentWhenTreeIsEmpty() throws Exception {
        when(service.getFullTree()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tree"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnConflictWhenDeletingRoot() throws Exception {
        doThrow(new RootNodeDeletionException()).when(service).delete(1L);

        mockMvc.perform(delete("/api/tree/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete root node"));
    }

    @Test
    void shouldReportFieldErrorsAsProblemDetail() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("", "", null);

        mockMvc.perform(post("/api/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.content").value("Content is required"));
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectTooLongName() throws Exception {
        TreeNodeRequest request = new TreeNodeRequest("x".repeat(TreeNodeRequest.NAME_MAX_LENGTH + 1), "c", 1L);

        mockMvc.perform(post("/api/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name must be at most 255 characters"));
    }

    @Test
    void shouldReturnBadRequestForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/tree/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMoveWithoutNodeId() throws Exception {
        mockMvc.perform(post("/api/tree/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nodeId").value("Node ID is required"));
    }

    @Test
    void shouldNotLeakInternalErrorDetails() throws Exception {
        when(service.getFullTree()).thenThrow(new RuntimeException("secret internals"));

        mockMvc.perform(get("/api/tree"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected server error"));
    }

    private TreeNodeResponse createResponse(Long id, String name, String content, Long parentId) {
        return new TreeNodeResponse(id, name, content, parentId, List.of(), null);
    }
}

