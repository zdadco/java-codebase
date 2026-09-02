package ru.zdadco.indexer.core.api;

import ru.zdadco.indexer.core.persistence.IndexJob;

public class IndexJobMapper {

    public IndexJobResponse toResponse(IndexJob job) {
        return new IndexJobResponse(
                job.getId(),
                job.getRepoPath(),
                job.getCommitSha(),
                job.getStatus().name(),
                job.getError(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }
}
