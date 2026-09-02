package ru.zdadco.indexer.core.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexedRepoRepository extends JpaRepository<IndexedRepo, String> {
}
