package ru.zdadco.indexer.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.zdadco.indexer.core.search.SearchHit;
import ru.zdadco.indexer.core.search.SearchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<SearchHit> search(
            @RequestParam("q") String query,
            @RequestParam(value = "repo_path", required = false) String repoPath,
            @RequestParam(value = "symbol_type", required = false) String symbolType,
            @RequestParam(value = "stereotype", required = false) String stereotype
    ) {
        return searchService.semanticSearch(query, repoPath, symbolType, stereotype);
    }
}
