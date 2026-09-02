package ru.zdadco.indexer.core.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceWalkerTest {

    private final JavaSourceWalker walker = new JavaSourceWalker();

    @Test
    void walksOnlyMainJavaSourcesAndSkipsTestsGeneratedAndBuildDirs(@TempDir Path root) throws Exception {
        Path main = root.resolve("module-a/src/main/java/com/example/App.java");
        Path nested = root.resolve("module-b/src/main/java/com/example/Nested.java");
        Path test = root.resolve("module-a/src/test/java/com/example/AppTest.java");
        Path generated = root.resolve("module-a/src/main/java/com/example/generated/QApp.java");
        Path target = root.resolve("module-a/target/classes/com/example/App.java");
        Path resource = root.resolve("module-a/src/main/resources/application.yml");

        Files.createDirectories(main.getParent());
        Files.createDirectories(nested.getParent());
        Files.createDirectories(test.getParent());
        Files.createDirectories(generated.getParent());
        Files.createDirectories(target.getParent());
        Files.createDirectories(resource.getParent());
        Files.writeString(main, "class App {}");
        Files.writeString(nested, "class Nested {}");
        Files.writeString(test, "class AppTest {}");
        Files.writeString(generated, "class QApp {}");
        Files.writeString(target, "class App {}");
        Files.writeString(resource, "spring: {}");

        List<Path> files = walker.findJavaSources(root);

        assertThat(files).containsExactlyInAnyOrder(main, nested);
    }
}
