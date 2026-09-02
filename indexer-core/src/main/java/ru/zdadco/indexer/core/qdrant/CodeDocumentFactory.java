package ru.zdadco.indexer.core.qdrant;

import org.springframework.ai.document.Document;
import ru.zdadco.indexer.core.chunker.CodeChunk;
import ru.zdadco.indexer.core.embedding.EmbeddingTextFormatter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeDocumentFactory {

    private final EmbeddingTextFormatter formatter;

    public CodeDocumentFactory(EmbeddingTextFormatter formatter) {
        this.formatter = formatter;
    }

    public Document toDocument(CodeChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        put(metadata, "repo_path", chunk.repoPath());
        put(metadata, "gitlab_project_id", chunk.gitlabProjectId());
        put(metadata, "commit_sha", chunk.commitSha());
        put(metadata, "branch", chunk.branch());
        put(metadata, "file_path", chunk.filePath());
        put(metadata, "package", chunk.packageName());
        put(metadata, "qualified_name", chunk.qualifiedName());
        put(metadata, "symbol_type", chunk.symbolType());
        put(metadata, "name", chunk.name());
        put(metadata, "signature", chunk.signature());
        put(metadata, "modifiers", String.join(",", chunk.modifiers()));
        put(metadata, "annotations", String.join(",", chunk.annotations()));
        put(metadata, "stereotype", chunk.stereotype());
        put(metadata, "http_method", chunk.httpMethod());
        put(metadata, "http_path", chunk.httpPath());
        put(metadata, "extends", chunk.extendsType());
        put(metadata, "implements", String.join(",", chunk.implementsTypes()));
        put(metadata, "calls", String.join(",", chunk.calls()));
        put(metadata, "injects", String.join(",", chunk.injects()));
        put(metadata, "javadoc_summary", chunk.javadocSummary());
        put(metadata, "line_start", chunk.lineStart());
        put(metadata, "line_end", chunk.lineEnd());
        put(metadata, "content_hash", chunk.contentHash());
        put(metadata, "indexed_at", Instant.now().toString());
        put(metadata, "language", "java");
        return new Document(chunk.pointId(), formatter.format(chunk), metadata);
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
