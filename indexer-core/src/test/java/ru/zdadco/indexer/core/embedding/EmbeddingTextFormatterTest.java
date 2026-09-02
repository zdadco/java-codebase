package ru.zdadco.indexer.core.embedding;

import org.junit.jupiter.api.Test;
import ru.zdadco.indexer.core.chunker.CodeChunk;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingTextFormatterTest {

    private final EmbeddingTextFormatter formatter = new EmbeddingTextFormatter();

    @Test
    void buildsNaturalLanguageContextPlusSource() {
        CodeChunk chunk = CodeChunk.builder()
                .repoPath("group/backend-service")
                .packageName("com.example.service")
                .qualifiedName("com.example.UserService.findById")
                .symbolType("method")
                .stereotype("service")
                .name("findById")
                .signature("findById(Long id): Optional<UserDto>")
                .annotations(List.of("@Transactional(readOnly=true)"))
                .calls(List.of("UserRepository.findById", "UserMapper.toDto"))
                .javadocSummary("Find user by id and map to DTO")
                .source("public Optional<UserDto> findById(Long id) { return Optional.empty(); }")
                .build();

        String text = formatter.format(chunk);

        assertThat(text).contains("Repository: group/backend-service");
        assertThat(text).contains("Package: com.example.service");
        assertThat(text).contains("QualifiedName: com.example.UserService.findById");
        assertThat(text).contains("Type: method");
        assertThat(text).contains("Stereotype: @Service");
        assertThat(text).contains("Signature: findById(Long id): Optional<UserDto>");
        assertThat(text).contains("Annotations: @Transactional(readOnly=true)");
        assertThat(text).contains("Calls: UserRepository.findById, UserMapper.toDto");
        assertThat(text).contains("Summary: Find user by id and map to DTO");
        assertThat(text).contains("---");
        assertThat(text).contains("public Optional<UserDto> findById(Long id)");
    }
}
