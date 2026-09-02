package ru.zdadco.indexer.core.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import ru.zdadco.indexer.core.persistence.IndexedRepo;
import ru.zdadco.indexer.core.persistence.IndexedRepoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private IndexedRepoRepository indexedRepoRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void appliesOptionalFiltersToSemanticSearch() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        searchService.semanticSearch("order creation", "group/backend", "method", "service");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("order creation");
        assertThat(captor.getValue().getFilterExpression().toString())
                .contains("repo_path")
                .contains("symbol_type")
                .contains("stereotype");
    }

    @Test
    void listsIndexedReposFromPostgres() {
        when(indexedRepoRepository.findAll()).thenReturn(List.of(
                IndexedRepo.builder().repoPath("a/one").latestCommitSha("1").lastIndexedAt(Instant.now()).build(),
                IndexedRepo.builder().repoPath("b/two").latestCommitSha("2").lastIndexedAt(Instant.now()).build()
        ));

        assertThat(searchService.listRepos()).containsExactly("a/one", "b/two");
    }

    @Test
    void findsCallersByPayloadCalls() {
        Document caller = new Document(
                "id-1",
                "caller",
                Map.of("qualified_name", "com.example.OrderService.create", "calls", "UserService.findById,OrderMapper.toDto")
        );
        Document other = new Document(
                "id-2",
                "other",
                Map.of("qualified_name", "com.example.Other.noop", "calls", "Logger.info")
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(caller, other));

        List<SearchHit> hits = searchService.getCallers("com.example.UserService.findById");

        assertThat(hits).extracting(SearchHit::id).containsExactly("id-1");
    }
}
