package com.ptc.contenttree.repository;

import com.ptc.contenttree.config.ContentTreeProperties;
import com.ptc.contenttree.model.TreeNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory node store backed by a JSON file. Every write rewrites the whole
 * file atomically (temp file + move), so a crash never leaves a half-written
 * tree behind.
 */
@Repository
public class TreeRepository {

    private static final Logger log = LoggerFactory.getLogger(TreeRepository.class);

    private final ObjectMapper objectMapper;
    private final Path dataFile;
    private final Map<Long, TreeNode> nodes = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TreeRepository(ObjectMapper objectMapper, ContentTreeProperties properties) {
        this.objectMapper = objectMapper;
        this.dataFile = properties.dataFile();
    }

    @PostConstruct
    public void init() {
        try {
            Path dataDir = dataFile.toAbsolutePath().getParent();
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                log.info("Created data directory: {}", dataDir);
            }

            if (Files.exists(dataFile) && Files.size(dataFile) > 0) {
                List<TreeNode> loaded = objectMapper.readValue(dataFile.toFile(), new TypeReference<List<TreeNode>>() {});
                for (TreeNode node : loaded) {
                    nodes.put(node.id(), node);
                    idGenerator.accumulateAndGet(node.id() + 1, Math::max);
                }
                log.info("Loaded {} nodes from {}", nodes.size(), dataFile);
            } else {
                log.info("No existing data found, starting with empty tree");
            }
        } catch (IOException | JacksonException e) {
            // Refuse to start: continuing with an empty tree would overwrite the
            // existing (possibly only slightly broken) file on the first write.
            throw new IllegalStateException("Failed to load tree data from " + dataFile.toAbsolutePath(), e);
        }
    }

    public synchronized TreeNode save(TreeNode node) {
        TreeNode toStore = node.id() == null ? node.withId(idGenerator.getAndIncrement()) : node;
        TreeNode previous = nodes.put(toStore.id(), toStore);
        try {
            saveToFile();
        } catch (UncheckedIOException e) {
            // keep memory and disk consistent
            if (previous == null) {
                nodes.remove(toStore.id());
            } else {
                nodes.put(previous.id(), previous);
            }
            throw e;
        }
        return toStore;
    }

    public Optional<TreeNode> findById(Long id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /** Snapshot of all nodes, ordered by id. */
    public List<TreeNode> findAll() {
        return nodes.values().stream()
                .sorted(Comparator.comparing(TreeNode::id))
                .toList();
    }

    public synchronized void deleteWithChildren(Long id) {
        if (!nodes.containsKey(id)) {
            return;
        }
        List<TreeNode> removed = collectSubtreeIds(id).stream()
                .map(nodes::remove)
                .filter(Objects::nonNull)
                .toList();
        try {
            saveToFile();
        } catch (UncheckedIOException e) {
            removed.forEach(n -> nodes.put(n.id(), n));
            throw e;
        }
    }

    private List<Long> collectSubtreeIds(Long rootId) {
        List<Long> ids = new ArrayList<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.push(rootId);
        while (!pending.isEmpty()) {
            Long current = pending.pop();
            ids.add(current);
            nodes.values().stream()
                    .filter(n -> current.equals(n.parentId()))
                    .forEach(child -> pending.push(child.id()));
        }
        return ids;
    }

    public boolean existsById(Long id) {
        return nodes.containsKey(id);
    }

    public boolean hasRoot() {
        return nodes.values().stream().anyMatch(TreeNode::isRoot);
    }

    public long count() {
        return nodes.size();
    }

    private void saveToFile() {
        try {
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), findAll());
            try {
                Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | JacksonException e) {
            throw new UncheckedIOException("Failed to save tree data to " + dataFile.toAbsolutePath(),
                    e instanceof IOException io ? io : new IOException(e));
        }
    }
}
