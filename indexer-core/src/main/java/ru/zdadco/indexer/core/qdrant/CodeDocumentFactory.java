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
        metadata.put("repo_path", chunk.repoPath());
        metadata.put("gitlab_project_id", chunk.gitlabProjectId());
        metadata.put("commit_sha", chunk.commitSha());
        metadata.put("branch", chunk.branch());
        metadata.put("file_path", chunk.filePath());
        metadata.put("package", chunk.packageName());
        metadata.put("qualified_name", chunk.qualifiedName());
        metadata.put("symbol_type", chunk.symbolType());
        metadata.put("name", chunk.name());
        metadata.put("signature", chunk.signature());
        metadata.put("modifiers", String.join(",", chunk.modifiers()));
        metadata.put("annotations", String.join(",", chunk.annotations()));
        metadata.put("stereotype", chunk.stereotype());
        metadata.put("http_method", chunk.httpMethod());
        metadata.put("http_path", chunk.httpPath());
        metadata.put("extends", chunk.extendsType());
        metadata.put("implements", String.join(",", chunk.implementsTypes()));
        metadata.put("calls", String.join(",", chunk.calls()));
        metadata.put("injects", String.join(",", chunk.injects()));
        metadata.put("javadoc_summary", chunk.javadocSummary());
        metadata.put("line_start", chunk.lineStart());
        metadata.put("line_end", chunk.lineEnd());
        metadata.put("content_hash", chunk.contentHash());
        metadata.put("indexed_at", Instant.now().toString());
        metadata.put("language", "java");
        return new Document(chunk.pointId(), formatter.format(chunk), metadata);
    }
}
