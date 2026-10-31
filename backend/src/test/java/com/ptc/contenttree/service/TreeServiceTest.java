package com.ptc.contenttree.service;

import com.ptc.contenttree.config.ContentTreeProperties;
import com.ptc.contenttree.dto.TreeNodeRequest;
import com.ptc.contenttree.dto.TreeNodeResponse;
import com.ptc.contenttree.exception.InvalidTreeOperationException;
import com.ptc.contenttree.exception.NodeNotFoundException;
import com.ptc.contenttree.exception.RootNodeDeletionException;
import com.ptc.contenttree.repository.TreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the service against a real, file-backed repository in a temp dir. */
class TreeServiceTest {

    @TempDir
    Path dir;

    private TreeService service;

    @BeforeEach
    void setUp() {
        TreeRepository repository = new TreeRepository(JsonMapper.builder().build(),
                new ContentTreeProperties(dir.resolve("tree.json"), List.of()));
        repository.init();
        service = new TreeService(repository);
    }

    private TreeNodeResponse create(String name, Long parentId) {
        return service.create(new TreeNodeRequest(name, name + " content", parentId));
    }

    @Test
    void emptyTreeHasNoRoot() {
        assertThat(service.getFullTree()).isEmpty();
        assertThat(service.search("x")).isEmpty();
    }

    @Test
    void createsRootAndChildrenAsNestedTree() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());
        create("A1", a.id());
        create("B", root.id());

        TreeNodeResponse tree = service.getFullTree().orElseThrow();

        assertThat(tree.name()).isEqualTo("Root");
        assertThat(tree.isMatch()).isNull();
        assertThat(tree.children()).extracting(TreeNodeResponse::name).containsExactly("A", "B");
        assertThat(tree.children().getFirst().children()).extracting(TreeNodeResponse::name).containsExactly("A1");
    }

    @Test
    void rejectsSecondRoot() {
        create("Root", null);

        assertThatThrownBy(() -> create("Other root", null))
                .isInstanceOf(InvalidTreeOperationException.class)
                .hasMessageContaining("Root node already exists");
    }

    @Test
    void rejectsUnknownParent() {
        create("Root", null);

        assertThatThrownBy(() -> create("Orphan", 99L))
                .isInstanceOf(InvalidTreeOperationException.class)
                .hasMessage("Parent node does not exist: 99");
    }

    @Test
    void updateChangesNameAndContent() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse child = create("Child", root.id());

        TreeNodeResponse updated = service.update(child.id(), new TreeNodeRequest("Renamed", "new", root.id()));

        assertThat(updated.name()).isEqualTo("Renamed");
        assertThat(service.getById(child.id()).content()).isEqualTo("new");
    }

    @Test
    void updateOfUnknownNodeFails() {
        assertThatThrownBy(() -> service.update(5L, new TreeNodeRequest("n", "c", null)))
                .isInstanceOf(NodeNotFoundException.class);
    }

    @Test
    void updateCannotCreateCycle() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());
        TreeNodeResponse a1 = create("A1", a.id());

        assertThatThrownBy(() -> service.update(a.id(), new TreeNodeRequest("A", "a", a1.id())))
                .isInstanceOf(InvalidTreeOperationException.class)
                .hasMessage("Cannot move node to its own descendant");
        assertThatThrownBy(() -> service.update(root.id(), new TreeNodeRequest("Root", "r", a.id())))
                .isInstanceOf(InvalidTreeOperationException.class);
        // the tree is still intact and can be built
        assertThat(service.getFullTree()).isPresent();
    }

    @Test
    void updateCannotDetachNodeToRootLevel() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());

        assertThatThrownBy(() -> service.update(a.id(), new TreeNodeRequest("A", "a", null)))
                .isInstanceOf(InvalidTreeOperationException.class);
    }

    @Test
    void moveReparentsNode() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());
        TreeNodeResponse b = create("B", root.id());

        service.moveNode(b.id(), a.id());

        TreeNodeResponse tree = service.getFullTree().orElseThrow();
        assertThat(tree.children()).extracting(TreeNodeResponse::name).containsExactly("A");
        assertThat(tree.children().getFirst().children()).extracting(TreeNodeResponse::name).containsExactly("B");
    }

    @Test
    void moveRejectsSelfDescendantAndUnknownTargets() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());
        TreeNodeResponse a1 = create("A1", a.id());

        assertThatThrownBy(() -> service.moveNode(a.id(), a.id()))
                .isInstanceOf(InvalidTreeOperationException.class);
        assertThatThrownBy(() -> service.moveNode(a.id(), a1.id()))
                .isInstanceOf(InvalidTreeOperationException.class);
        assertThatThrownBy(() -> service.moveNode(a.id(), 99L))
                .isInstanceOf(InvalidTreeOperationException.class);
        assertThatThrownBy(() -> service.moveNode(a.id(), null))
                .isInstanceOf(InvalidTreeOperationException.class);
        assertThatThrownBy(() -> service.moveNode(99L, root.id()))
                .isInstanceOf(NodeNotFoundException.class);
    }

    @Test
    void deleteRemovesSubtreeButNeverRoot() {
        TreeNodeResponse root = create("Root", null);
        TreeNodeResponse a = create("A", root.id());
        TreeNodeResponse a1 = create("A1", a.id());

        assertThatThrownBy(() -> service.delete(root.id())).isInstanceOf(RootNodeDeletionException.class);

        service.delete(a.id());

        assertThat(service.getFullTree().orElseThrow().children()).isEmpty();
        assertThatThrownBy(() -> service.getById(a1.id())).isInstanceOf(NodeNotFoundException.class);
        assertThatThrownBy(() -> service.delete(a.id())).isInstanceOf(NodeNotFoundException.class);
    }

    @Test
    void searchFlagsMatchesCaseInsensitivelyInNameOrContent() {
        TreeNodeResponse root = create("Root", null);
        service.create(new TreeNodeRequest("Documents", "important", root.id()));
        service.create(new TreeNodeRequest("Pictures", "holiday DOCS", root.id()));
        create("Music", root.id());

        TreeNodeResponse result = service.search("  Doc ").orElseThrow();

        assertThat(result.isMatch()).isFalse();
        assertThat(result.children())
                .extracting(TreeNodeResponse::name, TreeNodeResponse::isMatch)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Documents", true),
                        org.assertj.core.groups.Tuple.tuple("Pictures", true),
                        org.assertj.core.groups.Tuple.tuple("Music", false));
    }

    @Test
    void searchDoesNotLeakMatchFlagsIntoLaterRequests() {
        TreeNodeResponse root = create("Root", null);
        create("Doc", root.id());

        service.search("doc");

        TreeNodeResponse tree = service.getFullTree().orElseThrow();
        assertThat(tree.isMatch()).isNull();
        assertThat(tree.children().getFirst().isMatch()).isNull();
    }

    @Test
    void blankSearchReturnsPlainTree() {
        create("Root", null);

        assertThat(service.search("   ").orElseThrow().isMatch()).isNull();
    }
}
