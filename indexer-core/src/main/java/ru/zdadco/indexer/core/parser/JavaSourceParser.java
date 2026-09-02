package ru.zdadco.indexer.core.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import ru.zdadco.indexer.core.chunker.CodeChunk;
import ru.zdadco.indexer.core.metadata.SpringMetadataExtractor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public class JavaSourceParser {

    public static final int MAX_METHOD_CHARS = 6_000;

    private final SpringMetadataExtractor metadataExtractor = new SpringMetadataExtractor();
    private final ParserConfiguration configuration;

    public JavaSourceParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.configuration = configuration;
    }

    public List<CodeChunk> parse(
            String source,
            String filePath,
            String repoPath,
            String gitlabProjectId,
            String commitSha,
            String branch
    ) {
        CompilationUnit cu = parseCompilationUnit(source);
        String packageName = cu.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");

        List<CodeChunk> chunks = new ArrayList<>();
        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) {
            if (!(type.isClassOrInterfaceDeclaration() || type.isEnumDeclaration() || type.isRecordDeclaration())) {
                continue;
            }
            chunks.addAll(parseType(type, source, filePath, repoPath, gitlabProjectId, commitSha, branch, packageName));
        }
        return chunks;
    }

    private CompilationUnit parseCompilationUnit(String source) {
        ParseResult<CompilationUnit> result = new JavaParser(configuration).parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new ParseProblemException(result.getProblems());
        }
        return result.getResult().get();
    }

    private List<CodeChunk> parseType(
            TypeDeclaration<?> type,
            String source,
            String filePath,
            String repoPath,
            String gitlabProjectId,
            String commitSha,
            String branch,
            String packageName
    ) {
        String typeName = qualifiedTypeName(packageName, type);
        String stereotype = metadataExtractor.stereotype(type);
        List<String> injects = metadataExtractor.injects(type);
        String httpPrefix = metadataExtractor.classHttpPath(type);
        String extendsType = extendsType(type);
        List<String> implementsTypes = implementsTypes(type);
        List<CodeChunk> chunks = new ArrayList<>();

        List<MethodDeclaration> methods = type.getMethods();
        for (MethodDeclaration method : methods) {
            chunks.addAll(parseMethod(
                    method,
                    type,
                    typeName,
                    stereotype,
                    injects,
                    httpPrefix,
                    extendsType,
                    implementsTypes,
                    filePath,
                    repoPath,
                    gitlabProjectId,
                    commitSha,
                    branch,
                    packageName
            ));
        }

        boolean entity = metadataExtractor.hasAnnotation(type, "Entity");
        if (entity) {
            for (FieldDeclaration field : type.getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    chunks.add(buildChunk(
                            filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                            typeName + "." + variable.getNameAsString(),
                            "field",
                            variable.getNameAsString(),
                            field.toString().trim(),
                            modifiers(field),
                            annotations(field),
                            stereotype,
                            null,
                            null,
                            extendsType,
                            implementsTypes,
                            List.of(),
                            injects,
                            firstJavadocLine(field),
                            lineStart(field),
                            lineEnd(field),
                            field.toString()
                    ));
                }
            }
        }

        if (methods.isEmpty() || lineCount(type) < 40) {
            chunks.add(buildChunk(
                    filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                    typeName,
                    symbolType(type),
                    type.getNameAsString(),
                    type.getNameAsString(),
                    modifiers(type),
                    annotations(type),
                    stereotype,
                    null,
                    null,
                    extendsType,
                    implementsTypes,
                    List.of(),
                    injects,
                    firstJavadocLine(type),
                    lineStart(type),
                    lineEnd(type),
                    type.toString()
            ));
        }
        return chunks;
    }

    private List<CodeChunk> parseMethod(
            MethodDeclaration method,
            TypeDeclaration<?> type,
            String typeName,
            String stereotype,
            List<String> injects,
            String httpPrefix,
            String extendsType,
            List<String> implementsTypes,
            String filePath,
            String repoPath,
            String gitlabProjectId,
            String commitSha,
            String branch,
            String packageName
    ) {
        String sourceSlice = method.toString();
        String qualifiedName = typeName + "." + method.getNameAsString();
        List<String> calls = extractCalls(method);
        SpringMetadataExtractor.HttpMapping mapping = metadataExtractor.httpMapping(method, httpPrefix);

        if (sourceSlice.length() <= MAX_METHOD_CHARS) {
            return List.of(buildChunk(
                    filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                    qualifiedName,
                    "method",
                    method.getNameAsString(),
                    signature(method),
                    modifiers(method),
                    annotations(method),
                    stereotype,
                    mapping.method(),
                    mapping.path(),
                    extendsType,
                    implementsTypes,
                    calls,
                    injects,
                    firstJavadocLine(method),
                    lineStart(method),
                    lineEnd(method),
                    sourceSlice
            ));
        }

        List<CodeChunk> parts = new ArrayList<>();
        if (method.getBody().isEmpty()) {
            parts.add(buildChunk(
                    filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                    qualifiedName,
                    "method",
                    method.getNameAsString(),
                    signature(method),
                    modifiers(method),
                    annotations(method),
                    stereotype,
                    mapping.method(),
                    mapping.path(),
                    extendsType,
                    implementsTypes,
                    calls,
                    injects,
                    firstJavadocLine(method),
                    lineStart(method),
                    lineEnd(method),
                    sourceSlice.substring(0, Math.min(sourceSlice.length(), MAX_METHOD_CHARS))
            ));
            return parts;
        }

        List<Statement> statements = method.getBody().get().getStatements();
        StringBuilder current = new StringBuilder();
        int partStart = lineStart(method);
        int partEnd = lineStart(method);
        int partIndex = 0;
        for (Statement statement : statements) {
            String statementSource = statement.toString();
            if (!current.isEmpty() && current.length() + statementSource.length() > MAX_METHOD_CHARS) {
                parts.add(buildChunk(
                        filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                        qualifiedName,
                        "method",
                        method.getNameAsString(),
                        signature(method) + "#" + partIndex,
                        modifiers(method),
                        annotations(method),
                        stereotype,
                        mapping.method(),
                        mapping.path(),
                        extendsType,
                        implementsTypes,
                        calls,
                        injects,
                        firstJavadocLine(method),
                        partStart,
                        partEnd,
                        wrapMethodPart(method, current.toString())
                ));
                current.setLength(0);
                partIndex++;
                partStart = lineStart(statement);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(statementSource);
            partEnd = lineEnd(statement);
        }
        if (!current.isEmpty()) {
            parts.add(buildChunk(
                    filePath, repoPath, gitlabProjectId, commitSha, branch, packageName,
                    qualifiedName,
                    "method",
                    method.getNameAsString(),
                    signature(method) + (partIndex == 0 ? "" : "#" + partIndex),
                    modifiers(method),
                    annotations(method),
                    stereotype,
                    mapping.method(),
                    mapping.path(),
                    extendsType,
                    implementsTypes,
                    calls,
                    injects,
                    firstJavadocLine(method),
                    partStart,
                    partEnd,
                    wrapMethodPart(method, current.toString())
            ));
        }
        return parts;
    }

    private String wrapMethodPart(MethodDeclaration method, String body) {
        return method.getDeclarationAsString() + " {\n" + body + "\n}";
    }

    private CodeChunk buildChunk(
            String filePath,
            String repoPath,
            String gitlabProjectId,
            String commitSha,
            String branch,
            String packageName,
            String qualifiedName,
            String symbolType,
            String name,
            String signature,
            List<String> modifiers,
            List<String> annotations,
            String stereotype,
            String httpMethod,
            String httpPath,
            String extendsType,
            List<String> implementsTypes,
            List<String> calls,
            List<String> injects,
            String javadocSummary,
            int lineStart,
            int lineEnd,
            String source
    ) {
        return CodeChunk.builder()
                .pointId(repoPath + ":" + commitSha + ":" + qualifiedName + ":" + lineStart)
                .repoPath(repoPath)
                .gitlabProjectId(gitlabProjectId)
                .commitSha(commitSha)
                .branch(branch)
                .filePath(filePath)
                .packageName(packageName)
                .qualifiedName(qualifiedName)
                .symbolType(symbolType)
                .name(name)
                .signature(signature)
                .modifiers(modifiers)
                .annotations(annotations)
                .stereotype(stereotype)
                .httpMethod(httpMethod)
                .httpPath(httpPath)
                .extendsType(extendsType)
                .implementsTypes(implementsTypes)
                .calls(calls)
                .injects(injects)
                .javadocSummary(javadocSummary)
                .lineStart(lineStart)
                .lineEnd(lineEnd)
                .contentHash("sha256:" + sha256(source))
                .source(source)
                .build();
    }

    private List<String> extractCalls(MethodDeclaration method) {
        List<String> calls = new ArrayList<>();
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String name = call.getNameAsString();
            String qualified = call.getScope()
                    .map(scope -> scope.toString() + "." + name)
                    .orElse(name);
            calls.add(qualified);
        });
        method.findAll(MethodReferenceExpr.class).forEach(ref ->
                calls.add(ref.getScope().toString() + "." + ref.getIdentifier())
        );
        return calls;
    }

    private String signature(MethodDeclaration method) {
        String params = method.getParameters().stream()
                .map(parameter -> parameter.getType().asString() + " " + parameter.getNameAsString())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return method.getNameAsString() + "(" + params + "): " + method.getType().asString();
    }

    private List<String> modifiers(Node node) {
        if (node instanceof MethodDeclaration method) {
            return method.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).toList();
        }
        if (node instanceof TypeDeclaration<?> type) {
            return type.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).toList();
        }
        if (node instanceof FieldDeclaration field) {
            return field.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).toList();
        }
        return List.of();
    }

    private List<String> annotations(Node node) {
        List<AnnotationExpr> annotations;
        if (node instanceof MethodDeclaration method) {
            annotations = method.getAnnotations();
        } else if (node instanceof TypeDeclaration<?> type) {
            annotations = type.getAnnotations();
        } else if (node instanceof FieldDeclaration field) {
            annotations = field.getAnnotations();
        } else {
            return List.of();
        }
        return annotations.stream()
                .map(annotation -> "@" + annotation.getName().getIdentifier())
                .toList();
    }

    private String firstJavadocLine(Node node) {
        return node.getComment()
                .flatMap(comment -> comment.isJavadocComment()
                        ? Optional.of(comment.asJavadocComment().parse().getDescription().toText().strip())
                        : Optional.empty())
                .map(text -> text.split("\\R", 2)[0].strip())
                .filter(text -> !text.isBlank())
                .orElse(null);
    }

    private int lineStart(Node node) {
        return node.getBegin().map(position -> position.line).orElse(1);
    }

    private int lineEnd(Node node) {
        return node.getEnd().map(position -> position.line).orElse(lineStart(node));
    }

    private int lineCount(TypeDeclaration<?> type) {
        return Math.max(0, lineEnd(type) - lineStart(type) + 1);
    }

    private String qualifiedTypeName(String packageName, TypeDeclaration<?> type) {
        String name = type.getNameAsString();
        Optional<Node> parent = type.getParentNode();
        while (parent.isPresent() && parent.get() instanceof TypeDeclaration<?> parentType) {
            name = parentType.getNameAsString() + "." + name;
            parent = parentType.getParentNode();
        }
        return packageName.isBlank() ? name : packageName + "." + name;
    }

    private String symbolType(TypeDeclaration<?> type) {
        if (type instanceof EnumDeclaration) {
            return "enum";
        }
        if (type instanceof RecordDeclaration) {
            return "record";
        }
        if (type instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface()) {
            return "interface";
        }
        return "class";
    }

    private String extendsType(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.getExtendedTypes().stream()
                    .findFirst()
                    .map(ClassOrInterfaceType::getNameAsString)
                    .orElse(null);
        }
        return null;
    }

    private List<String> implementsTypes(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.getImplementedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString)
                    .toList();
        }
        return List.of();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
