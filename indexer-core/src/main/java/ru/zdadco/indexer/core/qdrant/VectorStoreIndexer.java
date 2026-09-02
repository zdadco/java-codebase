package ru.zdadco.indexer.core.qdrant;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import ru.zdadco.indexer.core.chunker.CodeChunk;
import ru.zdadco.indexer.core.config.IndexerProperties;

import java.util.List;

public class VectorStoreIndexer {

    private final VectorStore vectorStore;
    private final CodeDocumentFactory documentFactory;
    private final IndexerProperties properties;

    public VectorStoreIndexer(
            VectorStore vectorStore,
            CodeDocumentFactory documentFactory,
            IndexerProperties properties
    ) {
        this.vectorStore = vectorStore;
        this.documentFactory = documentFactory;
        this.properties = properties;
    }

    public void replaceRepoSnapshot(String repoPath, List<CodeChunk> chunks) {
        vectorStore.delete(new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("repo_path"),
                new Filter.Value(repoPath)
        ));
        List<Document> documents = chunks.stream().map(documentFactory::toDocument).toList();
        int batchSize = Math.max(1, properties.getEmbedding().getBatchSize());
        for (int i = 0; i < documents.size(); i += batchSize) {
            vectorStore.add(documents.subList(i, Math.min(i + batchSize, documents.size())));
        }
    }
}
