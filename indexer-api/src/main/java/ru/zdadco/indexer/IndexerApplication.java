package ru.zdadco.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "ru.zdadco.indexer")
@EnableAsync
public class IndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndexerApplication.class, args);
    }
}
