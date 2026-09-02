package ru.zdadco.indexer.core.chunker;

import lombok.Builder;

import java.util.List;

@Builder
public record CodeChunk(
        String pointId,
        String repoPath,
        String gitlabProjectId,
        String commitSha,
        String branch,
        String filePath,
        String packageName,
        String qualifiedName,
        String symbolType,
        String name,
        String signature,
        List<String> modifiers,
        List<String> annotations,
        String stereotype,
        String httpMethod,
        String httpPath,
        String extendsType,
        List<String> implementsTypes,
        List<String> calls,
        List<String> injects,
        String javadocSummary,
        int lineStart,
        int lineEnd,
        String contentHash,
        String source
) {
    public CodeChunk {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        implementsTypes = implementsTypes == null ? List.of() : List.copyOf(implementsTypes);
        calls = calls == null ? List.of() : List.copyOf(calls);
        injects = injects == null ? List.of() : List.copyOf(injects);
    }
}
