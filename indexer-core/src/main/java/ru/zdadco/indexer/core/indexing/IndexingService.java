package ru.zdadco.indexer.core.indexing;

import ru.zdadco.indexer.core.chunker.CodeChunk;
import ru.zdadco.indexer.core.git.GitCheckoutService;
import ru.zdadco.indexer.core.parser.JavaSourceParser;
import ru.zdadco.indexer.core.parser.JavaSourceWalker;
import ru.zdadco.indexer.core.persistence.IndexedRepo;
import ru.zdadco.indexer.core.persistence.IndexedRepoRepository;
import ru.zdadco.indexer.core.qdrant.VectorStoreIndexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IndexingService {

    private final GitCheckoutService gitCheckoutService;
    private final JavaSourceWalker sourceWalker;
    private final JavaSourceParser sourceParser;
    private final VectorStoreIndexer vectorStoreIndexer;
    private final IndexedRepoRepository indexedRepoRepository;

    public IndexingService(
            GitCheckoutService gitCheckoutService,
            JavaSourceWalker sourceWalker,
            JavaSourceParser sourceParser,
            VectorStoreIndexer vectorStoreIndexer,
            IndexedRepoRepository indexedRepoRepository
    ) {
        this.gitCheckoutService = gitCheckoutService;
        this.sourceWalker = sourceWalker;
        this.sourceParser = sourceParser;
        this.vectorStoreIndexer = vectorStoreIndexer;
        this.indexedRepoRepository = indexedRepoRepository;
    }

    public int indexRepository(String repoUrl, String repoPath, String gitlabProjectId, String commitSha, String branch) {
        Path workDir = gitCheckoutService.checkout(repoUrl, commitSha);
        try {
            List<CodeChunk> chunks = new ArrayList<>();
            for (Path file : sourceWalker.findJavaSources(workDir)) {
                String source = Files.readString(file);
                String relative = workDir.relativize(file).toString().replace('\\', '/');
                chunks.addAll(sourceParser.parse(source, relative, repoPath, gitlabProjectId, commitSha, branch));
            }
            chunks.sort(Comparator.comparing(CodeChunk::qualifiedName));
            vectorStoreIndexer.replaceRepoSnapshot(repoPath, chunks);
            indexedRepoRepository.save(IndexedRepo.builder()
                    .repoPath(repoPath)
                    .latestCommitSha(commitSha)
                    .lastIndexedAt(Instant.now())
                    .build());
            return chunks.size();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to index " + repoPath, ex);
        }
    }
}
