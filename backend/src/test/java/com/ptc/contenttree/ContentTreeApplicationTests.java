package com.ptc.contenttree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest
class ContentTreeApplicationTests {

	@TempDir
	static Path dataDir;

	@DynamicPropertySource
	static void dataFile(DynamicPropertyRegistry registry) {
		registry.add("content-tree.data-file", () -> dataDir.resolve("tree.json").toString());
	}

	@Test
	void contextLoads() {
	}

}
