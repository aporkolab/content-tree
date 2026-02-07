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

        if (request.getParentId() != null && !request.getParentId().equals(node.getParentId())) {
            validateParent(request.getParentId());
        }

        node.setName(request.getName());
        node.setContent(request.getContent());
        node.setParentId(request.getParentId());

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

    public void moveNode(Long nodeId, Long newParentId) {
        // using findById directly here, slightly different pattern than delete()
        TreeNode node = repository.findById(nodeId).orElse(null);
        if (node == null) {
            throw new RuntimeException("Source node does not exist: " + nodeId);
        }

        if (newParentId != null) {
            if (!repository.existsById(newParentId)) {
                throw new IllegalArgumentException("Target parent does not exist: " + newParentId);
            }
            if (isDescendant(nodeId, newParentId)) {
                throw new IllegalArgumentException("Cannot move node to its own descendant");
            }
        }

        node.setParentId(newParentId);
        repository.save(node);
        log.debug("Moved node {} to parent {}", nodeId, newParentId);
    }

    public TreeNodeResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getFullTree();
        }

        String lowerQuery = query.toLowerCase();
        TreeNode root = repository.buildTree();
        if (root == null) {
            return null;
        }

        // Mark matching nodes in tree
        markMatches(root, lowerQuery);
        return toResponseWithChildren(root);
    }

    private boolean markMatches(TreeNode node, String query) {
        boolean nameMatch = node.getName().toLowerCase().contains(query);
        boolean contentMatch = node.getContent().toLowerCase().contains(query);
        boolean selfMatch = nameMatch || contentMatch;

        boolean childMatch = false;
        for (TreeNode child : node.getChildren()) {
            if (markMatches(child, query)) {
                childMatch = true;
            }
        }

        // a node is a "match" if it or any descendant matches
        node.setIsMatch(selfMatch);
        return selfMatch || childMatch;
    }

    private boolean isDescendant(Long ancestorId, Long nodeId) {
        // Walk up from nodeId checking if we hit ancestorId
        Long currentId = nodeId;
        while (currentId != null) {
            if (currentId.equals(ancestorId)) {
                return true;
            }
            TreeNode current = repository.findById(currentId).orElse(null);
            if (current == null) break;
            currentId = current.getParentId();
        }
        return false;
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
        response.setIsMatch(node.getIsMatch());
        return response;
    }

    private TreeNodeResponse toResponseWithChildren(TreeNode node) {
        TreeNodeResponse response = new TreeNodeResponse();
        response.setId(node.getId());
        response.setName(node.getName());
        response.setContent(node.getContent());
        response.setParentId(node.getParentId());
        response.setIsMatch(node.getIsMatch());

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
