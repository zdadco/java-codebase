package ru.zdadco.indexer.api.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.zdadco.indexer.core.api.CreateIndexJobRequest;
import ru.zdadco.indexer.core.api.IndexJobMapper;
import ru.zdadco.indexer.core.api.IndexJobResponse;
import ru.zdadco.indexer.core.indexing.IndexJobProcessor;
import ru.zdadco.indexer.core.indexing.IndexJobService;
import ru.zdadco.indexer.core.persistence.IndexJob;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/index-jobs")
public class IndexJobController {

    private final IndexJobService indexJobService;
    private final IndexJobProcessor indexJobProcessor;
    private final IndexJobMapper indexJobMapper;

    public IndexJobController(
            IndexJobService indexJobService,
            IndexJobProcessor indexJobProcessor,
            IndexJobMapper indexJobMapper
    ) {
        this.indexJobService = indexJobService;
        this.indexJobProcessor = indexJobProcessor;
        this.indexJobMapper = indexJobMapper;
    }

    @PostMapping
    public ResponseEntity<IndexJobResponse> create(@Valid @RequestBody CreateIndexJobRequest request) {
        IndexJob job = indexJobService.submit(
                request.gitlabProjectId(),
                request.repoPath(),
                request.repoUrl(),
                request.commitSha(),
                request.branch()
        );
        indexJobProcessor.processAsync(job.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(indexJobMapper.toResponse(job));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<IndexJobResponse> get(@PathVariable UUID jobId) {
        return indexJobService.get(jobId)
                .map(indexJobMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
