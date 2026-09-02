package ru.zdadco.indexer.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "indexed_repos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class IndexedRepo {

    @Id
    @Column(name = "repo_path", nullable = false, length = 512)
    @EqualsAndHashCode.Include
    private String repoPath;

    @Column(name = "latest_commit_sha", nullable = false, length = 64)
    private String latestCommitSha;

    @Column(name = "last_indexed_at", nullable = false)
    private Instant lastIndexedAt;
}
