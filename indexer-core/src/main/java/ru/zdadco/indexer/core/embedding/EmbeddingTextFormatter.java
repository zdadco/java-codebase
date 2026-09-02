package ru.zdadco.indexer.core.embedding;

import ru.zdadco.indexer.core.chunker.CodeChunk;

import java.util.Locale;

public class EmbeddingTextFormatter {

    public String format(CodeChunk chunk) {
        String stereotype = formatStereotype(chunk.stereotype());
        String annotations = chunk.annotations().isEmpty() ? "-" : String.join(", ", chunk.annotations());
        String calls = chunk.calls().isEmpty() ? "-" : String.join(", ", chunk.calls());
        String summary = chunk.javadocSummary() == null || chunk.javadocSummary().isBlank()
                ? "-"
                : chunk.javadocSummary();

        return """
                Repository: %s
                Package: %s
                QualifiedName: %s
                Type: %s | Stereotype: %s
                Signature: %s
                Annotations: %s
                Calls: %s
                Summary: %s
                ---
                %s
                """.formatted(
                nullToEmpty(chunk.repoPath()),
                nullToEmpty(chunk.packageName()),
                nullToEmpty(chunk.qualifiedName()),
                nullToEmpty(chunk.symbolType()),
                stereotype,
                nullToEmpty(chunk.signature()),
                annotations,
                calls,
                summary,
                nullToEmpty(chunk.source())
        ).trim();
    }

    private String formatStereotype(String stereotype) {
        if (stereotype == null || stereotype.isBlank()) {
            return "-";
        }
        if (stereotype.startsWith("@")) {
            return stereotype;
        }
        String[] parts = stereotype.split("-");
        StringBuilder annotation = new StringBuilder("@");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            annotation.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1));
        }
        return annotation.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
