package ru.zdadco.indexer.core.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record IndexJobResponse(
        @JsonProperty("job_id") UUID jobId,
        @JsonProperty("repo_path") String repoPath,
        @JsonProperty("commit_sha") String commitSha,
        String status,
        String error,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt
) {
}
