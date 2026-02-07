package com.ptc.contenttree.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveNodeRequest {

    @NotNull(message = "Node ID is required")
    private Long nodeId;

    private Long newParentId;
}
