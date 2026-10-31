package com.ptc.contenttree.controller;

import com.ptc.contenttree.dto.MoveNodeRequest;
import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.service.TreeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tree")
public class TreeController {

    private final TreeService service;

    public TreeController(TreeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<TreeNodeResponse> getTree() {
        return service.getFullTree()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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
        service.moveNode(request.nodeId(), request.newParentId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<TreeNodeResponse> search(@RequestParam(defaultValue = "") String query) {
        return service.search(query)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
