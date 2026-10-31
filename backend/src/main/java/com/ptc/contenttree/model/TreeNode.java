package com.ptc.contenttree.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single node as it is stored. Immutable, so instances handed out by the
 * repository can be shared safely between requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TreeNode(Long id, String name, String content, Long parentId) {

    public TreeNode withId(Long newId) {
        return new TreeNode(newId, name, content, parentId);
    }

    public TreeNode withParentId(Long newParentId) {
        return new TreeNode(id, name, content, newParentId);
    }

    public boolean isRoot() {
        return parentId == null;
    }
}
