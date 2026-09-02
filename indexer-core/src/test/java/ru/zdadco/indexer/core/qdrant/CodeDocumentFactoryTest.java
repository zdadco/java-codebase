package ru.zdadco.indexer.core.qdrant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import ru.zdadco.indexer.core.chunker.CodeChunk;
import ru.zdadco.indexer.core.embedding.EmbeddingTextFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CodeDocumentFactoryTest {

    private final CodeDocumentFactory factory = new CodeDocumentFactory(new EmbeddingTextFormatter());

    @Test
    void toDocumentAllowsOptionalNullMetadataFields() {
        CodeChunk chunk = CodeChunk.builder()
                .pointId("group/backend:abc:com.example.UserId:1")
                .repoPath("group/backend")
                .commitSha("abc")
                .filePath("src/main/java/com/example/UserId.java")
                .packageName("com.example")
                .qualifiedName("com.example.UserId")
                .symbolType("record")
                .name("UserId")
                .signature("UserId")
                .stereotype(null)
                .httpMethod(null)
                .httpPath(null)
                .extendsType(null)
                .javadocSummary(null)
                .gitlabProjectId(null)
                .branch(null)
                .source("public record UserId(Long value) {}")
                .contentHash("sha256:abc")
                .lineStart(1)
                .lineEnd(3)
                .build();

        Document document = factory.toDocument(chunk);

        assertThat(document.getId()).isEqualTo(chunk.pointId());
        assertThat(document.getMetadata()).doesNotContainValue(null);
        assertThat(document.getMetadata()).doesNotContainKeys(
                "stereotype", "http_method", "http_path", "extends", "javadoc_summary", "gitlab_project_id", "branch"
        );
        assertThat(document.getMetadata()).containsEntry("language", "java");
        assertThat(document.getMetadata()).containsEntry("symbol_type", "record");
    }

    @Test
    void toDocumentDoesNotThrowWhenOptionalFieldsAreNull() {
        CodeChunk chunk = CodeChunk.builder()
                .pointId("id")
                .repoPath("repo")
                .qualifiedName("Q")
                .symbolType("method")
                .name("m")
                .source("void m() {}")
                .build();

        assertThatCode(() -> factory.toDocument(chunk)).doesNotThrowAnyException();
    }
}
