package com.ptc.contenttree.repository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.ptc.contenttree.model.TreeNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class TreeRepository {

    private static final Logger log = LoggerFactory.getLogger(TreeRepository.class);
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "data/tree.json";

    private final ObjectMapper objectMapper;
    private Map<Long, TreeNode> nodes = new ConcurrentHashMap<>();
    private AtomicLong idGenerator = new AtomicLong(1);

    public TreeRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                log.info("Created data directory: {}", dataDir.toAbsolutePath());
            }

            File file = new File(DATA_FILE);
            if (file.exists() && file.length() > 0) {
                List<TreeNode> loaded = objectMapper.readValue(file, new TypeReference<List<TreeNode>>() {});
                for (TreeNode node : loaded) {
                    nodes.put(node.getId(), node);
                    if (node.getId() >= idGenerator.get()) {
                        idGenerator.set(node.getId() + 1);
                    }
                }
                log.info("Loaded {} nodes from {}", nodes.size(), DATA_FILE);
            } else {
                log.info("No existing data found, starting with empty tree");
            }
        } catch (Exception e) {
            log.error("Failed to load tree data: {}", e.getMessage());
        }
    }

    public TreeNode save(TreeNode node) {
        if (node.getId() == null) {
            node.setId(idGenerator.getAndIncrement());
        }
        nodes.put(node.getId(), node);
        saveToFile();
        return node;
    }

    public Optional<TreeNode> findById(Long id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public List<TreeNode> findAll() {
        return new ArrayList<>(nodes.values());
    }

    public List<TreeNode> getChildren(Long parentId) {
        return nodes.values().stream()
                .filter(n -> Objects.equals(n.getParentId(), parentId))
                .collect(Collectors.toList());
    }

    public void deleteWithChildren(Long id) {
        TreeNode node = nodes.get(id);
        if (node != null) {
            // recursively delete children first
            getChildren(id).forEach(child -> deleteWithChildren(child.getId()));
            nodes.remove(id);
            saveToFile();
        }
    }

    public void delete(Long id) {
        nodes.remove(id);
        saveToFile();
    }

    public TreeNode buildTree() {
        // find root node (parentId is null)
        TreeNode root = nodes.values().stream()
                .filter(n -> n.getParentId() == null)
                .findFirst()
                .orElse(null);

        if (root == null) {
            return null;
        }

        buildChildren(root);
        return root;
    }

    private void buildChildren(TreeNode parent) {
        List<TreeNode> children = getChildren(parent.getId());
        parent.setChildren(children);
        for (TreeNode child : children) {
            buildChildren(child);
        }
    }

    private void saveToFile() {
        try {
            List<TreeNode> allNodes = new ArrayList<>(nodes.values());
            // clear children references before saving to avoid duplication
            for (TreeNode node : allNodes) {
                node.setChildren(new ArrayList<>());
                node.setIsMatch(null);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), allNodes);
        } catch (Exception e) {
            log.error("Failed to save tree data: {}", e.getMessage());
        }
    }

    public boolean existsById(Long id) {
        return nodes.containsKey(id);
    }

    public long count() {
        return nodes.size();
    }
}
