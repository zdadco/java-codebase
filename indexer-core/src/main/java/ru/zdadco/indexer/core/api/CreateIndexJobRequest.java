package ru.zdadco.indexer.core.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateIndexJobRequest(
        @NotBlank @JsonProperty("gitlab_project_id") String gitlabProjectId,
        @NotBlank @JsonProperty("repo_path") String repoPath,
        @NotBlank @JsonProperty("repo_url") String repoUrl,
        @NotBlank @JsonProperty("commit_sha") String commitSha,
        @NotBlank String branch
) {
}
