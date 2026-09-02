package ru.zdadco.indexer.core.search;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import ru.zdadco.indexer.core.persistence.IndexedRepo;
import ru.zdadco.indexer.core.persistence.IndexedRepoRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchService {

    private final VectorStore vectorStore;
    private final IndexedRepoRepository indexedRepoRepository;

    public SearchService(VectorStore vectorStore, IndexedRepoRepository indexedRepoRepository) {
        this.vectorStore = vectorStore;
        this.indexedRepoRepository = indexedRepoRepository;
    }

    public List<SearchHit> semanticSearch(String query, String repoPath, String symbolType, String stereotype) {
        List<String> filters = new ArrayList<>();
        if (repoPath != null && !repoPath.isBlank()) {
            filters.add("repo_path == '" + escape(repoPath) + "'");
        }
        if (symbolType != null && !symbolType.isBlank()) {
            filters.add("symbol_type == '" + escape(symbolType) + "'");
        }
        if (stereotype != null && !stereotype.isBlank()) {
            filters.add("stereotype == '" + escape(stereotype) + "'");
        }
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(10);
        if (!filters.isEmpty()) {
            builder.filterExpression(String.join(" && ", filters));
        }
        return vectorStore.similaritySearch(builder.build()).stream().map(this::toHit).toList();
    }

    public List<SearchHit> getSymbol(String qualifiedName) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(qualifiedName)
                        .topK(5)
                        .filterExpression("qualified_name == '" + escape(qualifiedName) + "'")
                        .build())
                .stream()
                .map(this::toHit)
                .toList();
    }

    public List<String> listRepos() {
        return indexedRepoRepository.findAll().stream().map(IndexedRepo::getRepoPath).toList();
    }

    public List<SearchHit> findByAnnotation(String annotation) {
        String needle = normalizeAnnotation(annotation);
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(annotation)
                        .topK(50)
                        .build())
                .stream()
                .filter(document -> containsToken(metadataString(document, "annotations"), needle))
                .map(this::toHit)
                .toList();
    }

    public List<SearchHit> getCallers(String qualifiedName) {
        String simpleName = qualifiedName.contains(".")
                ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
                : qualifiedName;
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(qualifiedName)
                        .topK(50)
                        .build())
                .stream()
                .filter(document -> {
                    String calls = metadataString(document, "calls");
                    return containsToken(calls, qualifiedName) || containsToken(calls, simpleName);
                })
                .map(this::toHit)
                .toList();
    }

    private SearchHit toHit(Document document) {
        Map<String, Object> metadata = document.getMetadata() == null
                ? Map.of()
                : new LinkedHashMap<>(document.getMetadata());
        return new SearchHit(document.getId(), document.getText(), document.getScore(), metadata);
    }

    private String metadataString(Document document, String key) {
        Object value = document.getMetadata() == null ? null : document.getMetadata().get(key);
        return value == null ? "" : value.toString();
    }

    private boolean containsToken(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String normalizeAnnotation(String annotation) {
        String value = annotation.strip();
        if (!value.startsWith("@")) {
            value = "@" + value;
        }
        return value;
    }

    private String escape(String value) {
        return value.replace("'", "\\'");
    }
}
