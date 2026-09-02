package ru.zdadco.indexer.core.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import ru.zdadco.indexer.core.config.IndexerProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitCheckoutService {

    private final IndexerProperties properties;

    public GitCheckoutService(IndexerProperties properties) {
        this.properties = properties;
    }

    public Path checkout(String repoUrl, String commitSha) {
        try {
            Path workDir = Files.createTempDirectory("java-kb-");
            var clone = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(workDir.toFile())
                    .setNoCheckout(true);
            if (properties.getGitlabToken() != null && !properties.getGitlabToken().isBlank()) {
                clone.setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", properties.getGitlabToken()));
            }
            try (Git git = clone.call()) {
                git.checkout().setName(commitSha).call();
            }
            return workDir;
        } catch (GitAPIException | IOException ex) {
            throw new IllegalStateException("Failed to checkout " + repoUrl + " @ " + commitSha, ex);
        }
    }
}
