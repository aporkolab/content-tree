package com.ptc.contenttree.repository;

import com.ptc.contenttree.config.ContentTreeProperties;
import com.ptc.contenttree.model.TreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TreeRepositoryTest {

    @TempDir
    Path dir;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private Path dataFile;

    @BeforeEach
    void setUp() {
        dataFile = dir.resolve("nested/tree.json");
    }

    private TreeRepository newRepository() {
        TreeRepository repository = new TreeRepository(objectMapper,
                new ContentTreeProperties(dataFile, List.of("http://localhost:4200")));
        repository.init();
        return repository;
    }

    @Test
    void startsEmptyAndCreatesDataDirectory() {
        TreeRepository repository = newRepository();

        assertThat(repository.count()).isZero();
        assertThat(dataFile.getParent()).isDirectory();
    }

    @Test
    void assignsIncreasingIdsAndPersistsAcrossRestarts() {
        TreeRepository repository = newRepository();
        TreeNode root = repository.save(new TreeNode(null, "Root", "r", null));
        TreeNode child = repository.save(new TreeNode(null, "Child", "c", root.id()));

        assertThat(root.id()).isEqualTo(1L);
        assertThat(child.id()).isEqualTo(2L);

        TreeRepository reloaded = newRepository();
        assertThat(reloaded.findAll()).containsExactly(root, child);
        // id generator continues after the highest loaded id
        assertThat(reloaded.save(new TreeNode(null, "Next", "n", 1L)).id()).isEqualTo(3L);
    }

    @Test
    void deleteWithChildrenRemovesWholeSubtreeOnly() {
        TreeRepository repository = newRepository();
        TreeNode root = repository.save(new TreeNode(null, "Root", "r", null));
        TreeNode a = repository.save(new TreeNode(null, "A", "a", root.id()));
        TreeNode a1 = repository.save(new TreeNode(null, "A1", "a1", a.id()));
        repository.save(new TreeNode(null, "A1x", "a1x", a1.id()));
        TreeNode b = repository.save(new TreeNode(null, "B", "b", root.id()));

        repository.deleteWithChildren(a.id());

        assertThat(repository.findAll()).containsExactly(root, b);
        assertThat(newRepository().findAll()).containsExactly(root, b);
    }

    @Test
    void deleteOfUnknownIdIsNoOp() {
        TreeRepository repository = newRepository();
        repository.save(new TreeNode(null, "Root", "r", null));

        repository.deleteWithChildren(42L);

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void refusesToStartWithCorruptDataFile() throws IOException {
        Files.createDirectories(dataFile.getParent());
        Files.writeString(dataFile, "{ not json");

        assertThatThrownBy(this::newRepository)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load tree data");
        // the broken file must be left untouched for manual recovery
        assertThat(Files.readString(dataFile)).isEqualTo("{ not json");
    }

    @Test
    void failedWriteKeepsMemoryConsistentWithDisk() throws IOException {
        TreeRepository repository = newRepository();
        TreeNode root = repository.save(new TreeNode(null, "Root", "r", null));
        // a directory in place of the temp file makes the next write fail
        Files.createDirectory(dataFile.resolveSibling("tree.json.tmp"));

        assertThatThrownBy(() -> repository.save(new TreeNode(null, "Child", "c", root.id())))
                .isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> repository.save(root.withParentId(99L)))
                .isInstanceOf(UncheckedIOException.class);

        assertThat(repository.findAll()).containsExactly(root);
    }

    @Test
    void hasRootReflectsStoredNodes() {
        TreeRepository repository = newRepository();
        assertThat(repository.hasRoot()).isFalse();

        repository.save(new TreeNode(null, "Root", "r", null));

        assertThat(repository.hasRoot()).isTrue();
    }
}
