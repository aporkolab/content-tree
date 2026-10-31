package com.ptc.contenttree.dto;

import jakarta.validation.constraints.NotNull;

public record MoveNodeRequest(
        @NotNull(message = "Node ID is required") Long nodeId,
        Long newParentId) {
}
