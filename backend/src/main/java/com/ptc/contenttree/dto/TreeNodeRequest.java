package com.ptc.contenttree.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TreeNodeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = TreeNodeRequest.NAME_MAX_LENGTH, message = "Name must be at most {max} characters")
        String name,

        @NotBlank(message = "Content is required")
        @Size(max = TreeNodeRequest.CONTENT_MAX_LENGTH, message = "Content must be at most {max} characters")
        String content,

        Long parentId) {

    public static final int NAME_MAX_LENGTH = 255;
    public static final int CONTENT_MAX_LENGTH = 10_000;
}
