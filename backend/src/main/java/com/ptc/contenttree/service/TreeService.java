package com.ptc.contenttree.service;

import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.exception.NodeNotFoundException;
import com.ptc.contenttree.model.TreeNode;
import com.ptc.contenttree.repository.TreeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeService {

    private static final Logger log = LoggerFactory.getLogger(TreeService.class);
    private final TreeRepository repository;

    public TreeNodeResponse create(TreeNodeRequest request) {
        if (request.getParentId() != null) {
            validateParent(request.getParentId());
        }

        TreeNode node = new TreeNode();
        node.setName(request.getName());
        node.setContent(request.getContent());
        node.setParentId(request.getParentId());

        TreeNode saved = repository.save(node);
        log.info("Created node: {} under parent: {}", saved.getId(), saved.getParentId());
        return toResponse(saved);
    }

    public TreeNodeResponse update(Long id, TreeNodeRequest request) {
        TreeNode node = repository.findById(id)
                .orElseThrow(() -> new NodeNotFoundException("Node not found: " + id));

        node.setName(request.getName());
        node.setContent(request.getContent());

        TreeNode saved = repository.save(node);
        log.info("Updated node: {}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        TreeNode node = repository.findById(id)
                .orElseThrow(() -> new NodeNotFoundException("Node not found: " + id));

        // dont allow deleting root if it has children
        if (node.getParentId() == null && !repository.getChildren(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete root node with children");
        }

        log.info("Deleting node {} with all children", id);
        repository.deleteWithChildren(id);
    }

    public TreeNodeResponse getById(Long id) {
        TreeNode node = repository.findById(id)
                .orElseThrow(() -> new NodeNotFoundException("Node not found: " + id));
        return toResponse(node);
    }

    public TreeNodeResponse getFullTree() {
        TreeNode root = repository.buildTree();
        if (root == null) {
            return null;
        }
        return toResponseWithChildren(root);
    }

    private void validateParent(Long parentId) {
        if (!repository.existsById(parentId)) {
            throw new IllegalArgumentException("Parent node does not exist: " + parentId);
        }
    }

    private TreeNodeResponse toResponse(TreeNode node) {
        TreeNodeResponse response = new TreeNodeResponse();
        response.setId(node.getId());
        response.setName(node.getName());
        response.setContent(node.getContent());
        response.setParentId(node.getParentId());
        response.setChildren(new ArrayList<>());
        return response;
    }

    private TreeNodeResponse toResponseWithChildren(TreeNode node) {
        TreeNodeResponse response = new TreeNodeResponse();
        response.setId(node.getId());
        response.setName(node.getName());
        response.setContent(node.getContent());
        response.setParentId(node.getParentId());

        List<TreeNodeResponse> childResponses = new ArrayList<>();
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                childResponses.add(toResponseWithChildren(child));
            }
        }
        response.setChildren(childResponses);
        return response;
    }
}
