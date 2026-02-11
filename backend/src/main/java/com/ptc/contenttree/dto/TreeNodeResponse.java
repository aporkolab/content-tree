package com.ptc.contenttree.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TreeNodeResponse {

    private Long id;
    private String name;
    private String content;
    private Long parentId;
    private List<TreeNodeResponse> children;
    private Boolean isMatch;
}
