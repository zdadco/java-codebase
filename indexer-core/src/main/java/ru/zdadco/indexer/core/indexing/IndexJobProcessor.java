package ru.zdadco.indexer.core.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.UUID;

public class IndexJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(IndexJobProcessor.class);

    private final IndexJobService indexJobService;

    public IndexJobProcessor(IndexJobService indexJobService) {
        this.indexJobService = indexJobService;
    }

    @Async("indexJobExecutor")
    public void processAsync(UUID jobId) {
        try {
            indexJobService.process(jobId);
        } catch (RuntimeException ex) {
            log.warn("Index job {} failed: {}", jobId, ex.getMessage());
        }
    }
}
