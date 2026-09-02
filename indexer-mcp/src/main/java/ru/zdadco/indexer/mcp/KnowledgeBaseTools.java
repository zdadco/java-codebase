package ru.zdadco.indexer.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import ru.zdadco.indexer.core.search.SearchHit;
import ru.zdadco.indexer.core.search.SearchService;

import java.util.List;

@Component
public class KnowledgeBaseTools {

    private final SearchService searchService;

    public KnowledgeBaseTools(SearchService searchService) {
        this.searchService = searchService;
    }

    @McpTool(name = "semantic_search", description = "Semantic search over indexed Java code chunks")
    public List<SearchHit> semanticSearch(
            @McpToolParam(description = "Natural language query", required = true) String query,
            @McpToolParam(description = "Optional repo_path filter", required = false) String repoPath,
            @McpToolParam(description = "Optional symbol_type filter", required = false) String symbolType,
            @McpToolParam(description = "Optional stereotype filter", required = false) String stereotype
    ) {
        return searchService.semanticSearch(query, repoPath, symbolType, stereotype);
    }

    @McpTool(name = "get_symbol", description = "Exact lookup of a Java symbol by qualified name")
    public List<SearchHit> getSymbol(
            @McpToolParam(description = "Fully qualified symbol name", required = true) String qualifiedName
    ) {
        return searchService.getSymbol(qualifiedName);
    }

    @McpTool(name = "list_repos", description = "List indexed repositories")
    public List<String> listRepos() {
        return searchService.listRepos();
    }

    @McpTool(name = "find_by_annotation", description = "Find symbols that have the given annotation")
    public List<SearchHit> findByAnnotation(
            @McpToolParam(description = "Annotation name, e.g. @RestController", required = true) String annotation
    ) {
        return searchService.findByAnnotation(annotation);
    }

    @McpTool(name = "get_callers", description = "Find methods whose calls payload mentions the given symbol")
    public List<SearchHit> getCallers(
            @McpToolParam(description = "Qualified or simple method name", required = true) String qualifiedName
    ) {
        return searchService.getCallers(qualifiedName);
    }
}
