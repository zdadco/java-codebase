package ru.zdadco.indexer.core.indexing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.zdadco.indexer.core.persistence.IndexJob;
import ru.zdadco.indexer.core.persistence.IndexJobRepository;
import ru.zdadco.indexer.core.persistence.IndexJobStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexJobServiceTest {

    @Mock
    private IndexJobRepository indexJobRepository;

    @Mock
    private IndexingService indexingService;

    @InjectMocks
    private IndexJobService indexJobService;

    @Test
    void reusesExistingJobForSameRepoAndCommit() {
        IndexJob existing = IndexJob.builder()
                .id(UUID.randomUUID())
                .repoPath("group/backend")
                .commitSha("abc123")
                .status(IndexJobStatus.DONE)
                .build();
        when(indexJobRepository.findByRepoPathAndCommitSha("group/backend", "abc123"))
                .thenReturn(Optional.of(existing));

        IndexJob result = indexJobService.submit("123", "group/backend", "https://git/repo.git", "abc123", "master");

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(indexJobRepository, never()).save(any());
    }

    @Test
    void createsPendingJobWhenCommitWasNotIndexed() {
        when(indexJobRepository.findByRepoPathAndCommitSha("group/backend", "abc123"))
                .thenReturn(Optional.empty());
        when(indexJobRepository.save(any(IndexJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndexJob result = indexJobService.submit("123", "group/backend", "https://git/repo.git", "abc123", "master");

        ArgumentCaptor<IndexJob> captor = ArgumentCaptor.forClass(IndexJob.class);
        verify(indexJobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IndexJobStatus.PENDING);
        assertThat(result.getRepoPath()).isEqualTo("group/backend");
        assertThat(result.getCommitSha()).isEqualTo("abc123");
    }
}
