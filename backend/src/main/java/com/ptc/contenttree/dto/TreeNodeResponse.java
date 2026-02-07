package com.ptc.contenttree.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeResponse {

    private Long id;
    private String name;
    private String content;
    private Long parentId;
    private List<TreeNodeResponse> children;
    private Boolean isMatch;
}
