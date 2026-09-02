package ru.zdadco.indexer.core.parser;

import org.junit.jupiter.api.Test;
import ru.zdadco.indexer.core.chunker.CodeChunk;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void extractsMethodChunkWithParentContextAndMetadata() {
        String source = """
                package com.example.service;

                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                /**
                 * User application service.
                 */
                @Service
                public class UserService {

                    /**
                     * Find user by id and map to DTO
                     */
                    @Transactional(readOnly = true)
                    public java.util.Optional<UserDto> findById(Long id) {
                        var user = userRepository.findById(id);
                        return user.map(userMapper::toDto);
                    }
                }
                """;

        List<CodeChunk> chunks = parser.parse(
                source,
                "src/main/java/com/example/service/UserService.java",
                "group/backend-service",
                "123",
                "abc123",
                "master"
        );

        assertThat(chunks).isNotEmpty();
        CodeChunk method = chunks.stream()
                .filter(chunk -> "method".equals(chunk.symbolType()))
                .filter(chunk -> "findById".equals(chunk.name()))
                .findFirst()
                .orElseThrow();

        assertThat(method.packageName()).isEqualTo("com.example.service");
        assertThat(method.qualifiedName()).isEqualTo("com.example.service.UserService.findById");
        assertThat(method.stereotype()).isEqualTo("service");
        assertThat(method.annotations()).contains("@Transactional");
        assertThat(method.signature()).contains("findById");
        assertThat(method.javadocSummary()).isEqualTo("Find user by id and map to DTO");
        assertThat(method.calls()).anyMatch(call -> call.contains("findById"));
        assertThat(method.source()).contains("findById(Long id)");
        assertThat(method.lineStart()).isGreaterThan(0);
        assertThat(method.lineEnd()).isGreaterThanOrEqualTo(method.lineStart());
        assertThat(method.repoPath()).isEqualTo("group/backend-service");
        assertThat(method.commitSha()).isEqualTo("abc123");
        assertThat(method.contentHash()).startsWith("sha256:");
    }

    @Test
    void extractsHttpMappingAndInjectedTypes() {
        String source = """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.beans.factory.annotation.Autowired;

                @RestController
                public class UserController {
                    @Autowired
                    private UserService userService;

                    @GetMapping("/users/{id}")
                    public UserDto get(@PathVariable Long id) {
                        return userService.findById(id).orElseThrow();
                    }
                }
                """;

        List<CodeChunk> chunks = parser.parse(
                source,
                "src/main/java/com/example/web/UserController.java",
                "group/backend-service",
                "123",
                "abc123",
                "master"
        );

        CodeChunk method = chunks.stream()
                .filter(chunk -> "get".equals(chunk.name()))
                .findFirst()
                .orElseThrow();

        assertThat(method.stereotype()).isEqualTo("rest-controller");
        assertThat(method.httpMethod()).isEqualTo("GET");
        assertThat(method.httpPath()).isEqualTo("/users/{id}");
        assertThat(method.injects()).contains("UserService");
        assertThat(method.calls()).anyMatch(call -> call.contains("findById"));
    }

    @Test
    void extractsEntityFieldChunks() {
        String source = """
                package com.example.domain;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Column;

                @Entity
                public class User {
                    @Id
                    private Long id;

                    @Column(nullable = false)
                    private String email;
                }
                """;

        List<CodeChunk> chunks = parser.parse(
                source,
                "src/main/java/com/example/domain/User.java",
                "group/backend-service",
                "123",
                "abc123",
                "master"
        );

        assertThat(chunks)
                .filteredOn(chunk -> "field".equals(chunk.symbolType()))
                .extracting(CodeChunk::name)
                .contains("id", "email");
    }

    @Test
    void splitsOversizedMethodByBlocks() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            body.append("        if (value > ").append(i).append(") {\n");
            body.append("            System.out.println(\"branch ").append(i).append("\");\n");
            body.append("        }\n");
        }
        String source = """
                package com.example;

                public class Huge {
                    public void process(int value) {
                %s
                    }
                }
                """.formatted(body);

        List<CodeChunk> chunks = parser.parse(
                source,
                "src/main/java/com/example/Huge.java",
                "group/backend-service",
                "123",
                "abc123",
                "master"
        );

        List<CodeChunk> methodChunks = chunks.stream()
                .filter(chunk -> "process".equals(chunk.name()))
                .toList();
        assertThat(methodChunks.size()).isGreaterThan(1);
        assertThat(methodChunks).allMatch(chunk -> chunk.source().length() < 8_000);
    }
}
