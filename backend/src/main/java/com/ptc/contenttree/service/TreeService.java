package com.ptc.contenttree.service;

import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.exception.InvalidTreeOperationException;
import com.ptc.contenttree.exception.NodeNotFoundException;
import com.ptc.contenttree.exception.RootNodeDeletionException;
import com.ptc.contenttree.model.TreeNode;
import com.ptc.contenttree.repository.TreeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tree operations. Mutating methods are synchronized so the structural checks
 * (single root, no cycles) and the write happen atomically.
 */
@Service
public class TreeService {

    private static final Logger log = LoggerFactory.getLogger(TreeService.class);

    private final TreeRepository repository;

    public TreeService(TreeRepository repository) {
        this.repository = repository;
    }

    public synchronized TreeNodeResponse create(TreeNodeRequest request) {
        if (request.parentId() != null) {
            requireParentExists(request.parentId());
        } else if (repository.hasRoot()) {
            throw new InvalidTreeOperationException("Root node already exists, new nodes must have a parent");
        }

        TreeNode saved = repository.save(new TreeNode(null, request.name(), request.content(), request.parentId()));
        log.info("Created node: {} under parent: {}", saved.id(), saved.parentId());
        return toResponse(saved);
    }

    public synchronized TreeNodeResponse update(Long id, TreeNodeRequest request) {
        TreeNode node = findOrThrow(id);
        validateNewParent(node, request.parentId());

        TreeNode saved = repository.save(new TreeNode(id, request.name(), request.content(), request.parentId()));
        log.info("Updated node: {}", saved.id());
        return toResponse(saved);
    }

    public synchronized void delete(Long id) {
        TreeNode node = findOrThrow(id);
        if (node.isRoot()) {
            throw new RootNodeDeletionException();
        }

        log.info("Deleting node {} with all children", id);
        repository.deleteWithChildren(id);
    }

    public synchronized void moveNode(Long nodeId, Long newParentId) {
        TreeNode node = repository.findById(nodeId)
                .orElseThrow(() -> new NodeNotFoundException("Source node does not exist: " + nodeId));
        validateNewParent(node, newParentId);

        repository.save(node.withParentId(newParentId));
        log.debug("Moved node {} to parent {}", nodeId, newParentId);
    }

    public TreeNodeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /** Full tree, or empty when there is no root yet. */
    public Optional<TreeNodeResponse> getFullTree() {
        return buildTree(node -> null);
    }

    /**
     * Full tree with {@code isMatch} set on every node: {@code true} when its
     * name or content contains the query (case-insensitive). A blank query
     * returns the plain tree.
     */
    public Optional<TreeNodeResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return getFullTree();
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return buildTree(node -> contains(node.name(), needle) || contains(node.content(), needle));
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    /**
     * Builds the response tree from a snapshot, without touching the stored
     * nodes. {@code matcher} returns the value for {@code isMatch} (null to omit).
     */
    private Optional<TreeNodeResponse> buildTree(Function<TreeNode, Boolean> matcher) {
        List<TreeNode> all = repository.findAll();
        Map<Long, List<TreeNode>> childrenByParent = all.stream()
                .filter(Predicate.not(TreeNode::isRoot))
                .collect(Collectors.groupingBy(TreeNode::parentId));

        return all.stream()
                .filter(TreeNode::isRoot)
                .findFirst()
                .map(root -> toTreeResponse(root, childrenByParent, matcher));
    }

    private TreeNodeResponse toTreeResponse(TreeNode node, Map<Long, List<TreeNode>> childrenByParent,
                                            Function<TreeNode, Boolean> matcher) {
        List<TreeNodeResponse> children = childrenByParent.getOrDefault(node.id(), List.of()).stream()
                .map(child -> toTreeResponse(child, childrenByParent, matcher))
                .toList();
        return new TreeNodeResponse(node.id(), node.name(), node.content(), node.parentId(),
                children, matcher.apply(node));
    }

    /**
     * Checks that {@code node} may be re-parented under {@code newParentId}:
     * the parent must exist, must not be the node or one of its descendants,
     * and only one root may exist.
     */
    private void validateNewParent(TreeNode node, Long newParentId) {
        if (Objects.equals(node.parentId(), newParentId)) {
            return;
        }
        if (newParentId == null) {
            if (!repository.hasRoot()) {
                return;
            }
            throw new InvalidTreeOperationException("Cannot move node to root level, root already exists");
        }
        requireParentExists(newParentId);
        if (isSelfOrDescendant(node.id(), newParentId)) {
            throw new InvalidTreeOperationException("Cannot move node to its own descendant");
        }
    }

    /** Walks up from {@code candidateId} and reports whether {@code ancestorId} is on the path. */
    private boolean isSelfOrDescendant(Long ancestorId, Long candidateId) {
        Long currentId = candidateId;
        while (currentId != null) {
            if (currentId.equals(ancestorId)) {
                return true;
            }
            currentId = repository.findById(currentId).map(TreeNode::parentId).orElse(null);
        }
        return false;
    }

    private void requireParentExists(Long parentId) {
        if (!repository.existsById(parentId)) {
            throw new InvalidTreeOperationException("Parent node does not exist: " + parentId);
        }
    }

    private TreeNode findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NodeNotFoundException("Node not found: " + id));
    }

    private static TreeNodeResponse toResponse(TreeNode node) {
        return new TreeNodeResponse(node.id(), node.name(), node.content(), node.parentId(), List.of(), null);
    }
}
