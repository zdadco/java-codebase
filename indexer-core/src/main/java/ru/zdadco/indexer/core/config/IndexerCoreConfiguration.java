package ru.zdadco.indexer.core.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.zdadco.indexer.core.api.IndexJobMapper;
import ru.zdadco.indexer.core.embedding.EmbeddingTextFormatter;
import ru.zdadco.indexer.core.git.GitCheckoutService;
import ru.zdadco.indexer.core.indexing.IndexJobProcessor;
import ru.zdadco.indexer.core.indexing.IndexJobService;
import ru.zdadco.indexer.core.indexing.IndexingService;
import ru.zdadco.indexer.core.parser.JavaSourceParser;
import ru.zdadco.indexer.core.parser.JavaSourceWalker;
import ru.zdadco.indexer.core.persistence.IndexJobRepository;
import ru.zdadco.indexer.core.persistence.IndexedRepoRepository;
import ru.zdadco.indexer.core.qdrant.CodeDocumentFactory;
import ru.zdadco.indexer.core.qdrant.VectorStoreIndexer;
import ru.zdadco.indexer.core.search.SearchService;

@Configuration
@EnableConfigurationProperties(IndexerProperties.class)
public class IndexerCoreConfiguration {

    @Bean
    public JavaSourceWalker javaSourceWalker() {
        return new JavaSourceWalker();
    }

    @Bean
    public JavaSourceParser javaSourceParser() {
        return new JavaSourceParser();
    }

    @Bean
    public EmbeddingTextFormatter embeddingTextFormatter() {
        return new EmbeddingTextFormatter();
    }

    @Bean
    public CodeDocumentFactory codeDocumentFactory(EmbeddingTextFormatter embeddingTextFormatter) {
        return new CodeDocumentFactory(embeddingTextFormatter);
    }

    @Bean
    public GitCheckoutService gitCheckoutService(IndexerProperties indexerProperties) {
        return new GitCheckoutService(indexerProperties);
    }

    @Bean
    public VectorStoreIndexer vectorStoreIndexer(
            VectorStore vectorStore,
            CodeDocumentFactory codeDocumentFactory,
            IndexerProperties indexerProperties
    ) {
        return new VectorStoreIndexer(vectorStore, codeDocumentFactory, indexerProperties);
    }

    @Bean
    public IndexingService indexingService(
            GitCheckoutService gitCheckoutService,
            JavaSourceWalker javaSourceWalker,
            JavaSourceParser javaSourceParser,
            VectorStoreIndexer vectorStoreIndexer,
            IndexedRepoRepository indexedRepoRepository
    ) {
        return new IndexingService(
                gitCheckoutService,
                javaSourceWalker,
                javaSourceParser,
                vectorStoreIndexer,
                indexedRepoRepository
        );
    }

    @Bean
    public IndexJobService indexJobService(
            IndexJobRepository indexJobRepository,
            IndexingService indexingService
    ) {
        return new IndexJobService(indexJobRepository, indexingService);
    }

    @Bean
    public SearchService searchService(VectorStore vectorStore, IndexedRepoRepository indexedRepoRepository) {
        return new SearchService(vectorStore, indexedRepoRepository);
    }

    @Bean
    public IndexJobProcessor indexJobProcessor(IndexJobService indexJobService) {
        return new IndexJobProcessor(indexJobService);
    }

    @Bean
    public IndexJobMapper indexJobMapper() {
        return new IndexJobMapper();
    }
}
