package com.ptc.contenttree.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.util.List;

/**
 * Application settings, bound from the {@code content-tree.*} properties.
 *
 * @param dataFile            JSON file the tree is persisted to
 * @param corsAllowedOrigins  origins allowed to call {@code /api/**}
 */
@ConfigurationProperties(prefix = "content-tree")
public record ContentTreeProperties(
        @DefaultValue("data/tree.json") Path dataFile,
        @DefaultValue("http://localhost:4200") List<String> corsAllowedOrigins) {
}
