package ru.zdadco.indexer.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "indexer")
public class IndexerProperties {

    private String apiToken;
    private String gitlabToken;
    private Embedding embedding = new Embedding();

    @Getter
    @Setter
    public static class Embedding {
        private int batchSize = 50;
        private int maxMethodChars = 6000;
    }
}
