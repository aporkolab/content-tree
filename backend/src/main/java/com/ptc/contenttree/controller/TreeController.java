package com.ptc.contenttree.controller;

import com.ptc.contenttree.dto.MoveNodeRequest;
import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.service.TreeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tree")
@RequiredArgsConstructor
public class TreeController {

    private final TreeService service;

    @GetMapping
    public ResponseEntity<TreeNodeResponse> getTree() {
        return ResponseEntity.ok(service.getFullTree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreeNodeResponse> getNode(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<TreeNodeResponse> create(@Valid @RequestBody TreeNodeRequest request) {
        TreeNodeResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TreeNodeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TreeNodeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/move")
    public ResponseEntity<Void> moveNode(@Valid @RequestBody MoveNodeRequest request) {
        service.moveNode(request.getNodeId(), request.getNewParentId());
        return ResponseEntity.ok().build();
    }
}
