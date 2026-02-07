package com.ptc.contenttree.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Content is required")
    private String content;

    private Long parentId;
}
