package ru.zdadco.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.zdadco.indexer.api.config.InsecureSsl;

@SpringBootApplication(scanBasePackages = "ru.zdadco.indexer")
@EnableAsync
public class IndexerApplication {

    static {
        InsecureSsl.install();
    }

    public static void main(String[] args) {
        InsecureSsl.install();
        SpringApplication.run(IndexerApplication.class, args);
    }
}
