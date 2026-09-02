package ru.zdadco.indexer.core.indexing;

import ru.zdadco.indexer.core.persistence.IndexJob;
import ru.zdadco.indexer.core.persistence.IndexJobRepository;
import ru.zdadco.indexer.core.persistence.IndexJobStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class IndexJobService {

    private final IndexJobRepository indexJobRepository;
    private final IndexingService indexingService;

    public IndexJobService(IndexJobRepository indexJobRepository, IndexingService indexingService) {
        this.indexJobRepository = indexJobRepository;
        this.indexingService = indexingService;
    }

    public IndexJob submit(
            String gitlabProjectId,
            String repoPath,
            String repoUrl,
            String commitSha,
            String branch
    ) {
        return indexJobRepository.findByRepoPathAndCommitSha(repoPath, commitSha)
                .orElseGet(() -> indexJobRepository.save(IndexJob.builder()
                        .gitlabProjectId(gitlabProjectId)
                        .repoPath(repoPath)
                        .repoUrl(repoUrl)
                        .commitSha(commitSha)
                        .branch(branch)
                        .status(IndexJobStatus.PENDING)
                        .build()));
    }

    public Optional<IndexJob> get(UUID id) {
        return indexJobRepository.findById(id);
    }

    public void process(UUID jobId) {
        IndexJob job = indexJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown job " + jobId));
        if (job.getStatus() == IndexJobStatus.DONE) {
            return;
        }
        job.setStatus(IndexJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setError(null);
        indexJobRepository.save(job);
        try {
            indexingService.indexRepository(
                    job.getRepoUrl(),
                    job.getRepoPath(),
                    job.getGitlabProjectId(),
                    job.getCommitSha(),
                    job.getBranch()
            );
            job.setStatus(IndexJobStatus.DONE);
            job.setFinishedAt(Instant.now());
            indexJobRepository.save(job);
        } catch (RuntimeException ex) {
            job.setStatus(IndexJobStatus.FAILED);
            job.setError(ex.getMessage());
            job.setFinishedAt(Instant.now());
            indexJobRepository.save(job);
            throw ex;
        }
    }
}
