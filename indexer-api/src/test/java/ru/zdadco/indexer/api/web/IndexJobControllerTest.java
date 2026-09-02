package ru.zdadco.indexer.api.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.zdadco.indexer.core.api.IndexJobMapper;
import ru.zdadco.indexer.core.indexing.IndexJobProcessor;
import ru.zdadco.indexer.core.indexing.IndexJobService;
import ru.zdadco.indexer.core.persistence.IndexJob;
import ru.zdadco.indexer.core.persistence.IndexJobStatus;
import ru.zdadco.indexer.core.search.SearchService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IndexJobController.class)
class IndexJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndexJobService indexJobService;

    @MockitoBean
    private IndexJobProcessor indexJobProcessor;

    @MockitoBean
    private IndexJobMapper indexJobMapper;

    @MockitoBean
    private SearchService searchService;

    @Test
    @WithMockUser
    void acceptsIndexJobAndStartsAsyncProcessing() throws Exception {
        UUID jobId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        IndexJob job = IndexJob.builder()
                .id(jobId)
                .repoPath("group/backend-service")
                .commitSha("abc123")
                .status(IndexJobStatus.PENDING)
                .build();
        when(indexJobService.submit(eq("123"), eq("group/backend-service"), any(), eq("abc123"), eq("master")))
                .thenReturn(job);
        when(indexJobMapper.toResponse(job)).thenReturn(
                new ru.zdadco.indexer.core.api.IndexJobResponse(jobId, "group/backend-service", "abc123", "PENDING", null, null, null)
        );

        mockMvc.perform(post("/api/v1/index-jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gitlab_project_id": "123",
                                  "repo_path": "group/backend-service",
                                  "repo_url": "https://gitlab.example.com/group/backend-service.git",
                                  "commit_sha": "abc123",
                                  "branch": "master"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.job_id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(indexJobProcessor).processAsync(jobId);
    }
}
