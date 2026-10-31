package com.ptc.contenttree.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Node as returned by the API. {@code isMatch} is only set on search results:
 * {@code true} when the node itself matches the query.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TreeNodeResponse(
        Long id,
        String name,
        String content,
        Long parentId,
        List<TreeNodeResponse> children,
        Boolean isMatch) {
}
