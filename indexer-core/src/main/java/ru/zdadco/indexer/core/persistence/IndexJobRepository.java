package ru.zdadco.indexer.core.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndexJobRepository extends JpaRepository<IndexJob, UUID> {

    Optional<IndexJob> findByRepoPathAndCommitSha(String repoPath, String commitSha);
}
