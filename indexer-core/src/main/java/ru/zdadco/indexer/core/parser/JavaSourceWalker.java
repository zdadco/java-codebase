package ru.zdadco.indexer.core.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class JavaSourceWalker {

    public List<Path> findJavaSources(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::isMainJavaSource)
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to walk Java sources in " + root, ex);
        }
    }

    private boolean isMainJavaSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        if (!normalized.contains("/src/main/java/")) {
            return false;
        }
        if (normalized.contains("/src/test/")) {
            return false;
        }
        if (normalized.contains("/target/") || normalized.contains("/build/") || normalized.contains("/out/")) {
            return false;
        }
        return !normalized.contains("/generated/");
    }
}
