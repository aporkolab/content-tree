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
        } else if (repository.hasRoot()) {
            throw new IllegalArgumentException("Root node already exists, new nodes must have a parent");
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

        if (request.getParentId() == null && node.getParentId() != null && repository.hasRoot()) {
            throw new IllegalArgumentException("Cannot set parent to null, root node already exists");
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

        if (node.getParentId() == null) {
            throw new IllegalStateException("Cannot delete root node");
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
        clearMatchFlags(root);
        return toResponseWithChildren(root);
    }

    private void clearMatchFlags(TreeNode node) {
        node.setIsMatch(null);
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                clearMatchFlags(child);
            }
        }
    }

    public void moveNode(Long nodeId, Long newParentId) {
        TreeNode node = repository.findById(nodeId)
                .orElseThrow(() -> new NodeNotFoundException("Source node does not exist: " + nodeId));

        if (newParentId != null) {
            if (!repository.existsById(newParentId)) {
                throw new IllegalArgumentException("Target parent does not exist: " + newParentId);
            }
            if (isDescendant(nodeId, newParentId)) {
                throw new IllegalArgumentException("Cannot move node to its own descendant");
            }
        } else if (node.getParentId() != null && repository.hasRoot()) {
            // moving to root level but root already exists
            throw new IllegalArgumentException("Cannot move node to root level, root already exists");
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
        String name = node.getName() != null ? node.getName().toLowerCase() : "";
        String content = node.getContent() != null ? node.getContent().toLowerCase() : "";
        boolean selfMatch = name.contains(query) || content.contains(query);

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
