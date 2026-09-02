package ru.zdadco.indexer.core.metadata;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpringMetadataExtractor {

    private static final Map<String, String> STEREOTYPES = Map.of(
            "RestController", "rest-controller",
            "Controller", "controller",
            "Service", "service",
            "Repository", "repository",
            "Component", "component",
            "Configuration", "configuration",
            "Entity", "entity"
    );

    private static final Map<String, String> HTTP_METHODS = Map.of(
            "GetMapping", "GET",
            "PostMapping", "POST",
            "PutMapping", "PUT",
            "DeleteMapping", "DELETE",
            "PatchMapping", "PATCH"
    );

    public record HttpMapping(String method, String path) {
    }

    public String stereotype(TypeDeclaration<?> type) {
        for (var entry : STEREOTYPES.entrySet()) {
            if (hasAnnotation(type, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean hasAnnotation(NodeWithAnnotations<?> node, String annotationName) {
        return node.isAnnotationPresent(annotationName);
    }

    public List<String> injects(TypeDeclaration<?> type) {
        Set<String> injects = new LinkedHashSet<>();
        for (FieldDeclaration field : type.getFields()) {
            if (hasAnnotation(field, "Autowired") || hasAnnotation(field, "Inject") || hasAnnotation(field, "Resource")) {
                field.getVariables().forEach(variable -> injects.add(field.getCommonType().asString()));
            }
        }
        boolean springComponent = stereotype(type) != null;
        for (ConstructorDeclaration constructor : type.getConstructors()) {
            boolean autowired = hasAnnotation(constructor, "Autowired") || hasAnnotation(constructor, "Inject");
            if (autowired || (springComponent && type.getConstructors().size() == 1)) {
                constructor.getParameters().forEach(parameter -> injects.add(parameter.getType().asString()));
            }
        }
        if (springComponent) {
            type.getFields().forEach(field -> {
                if (!field.isStatic()) {
                    field.getVariables().forEach(variable -> injects.add(field.getCommonType().asString()));
                }
            });
        }
        return List.copyOf(injects);
    }

    public String classHttpPath(TypeDeclaration<?> type) {
        return annotationPath(type, "RequestMapping").orElse(null);
    }

    public HttpMapping httpMapping(MethodDeclaration method, String classPath) {
        for (var entry : HTTP_METHODS.entrySet()) {
            if (hasAnnotation(method, entry.getKey())) {
                String path = joinPaths(classPath, annotationPath(method, entry.getKey()).orElse(null));
                return new HttpMapping(entry.getValue(), path);
            }
        }
        if (hasAnnotation(method, "RequestMapping")) {
            String methodValue = requestMappingMethod(method).orElse("GET");
            String path = joinPaths(classPath, annotationPath(method, "RequestMapping").orElse(null));
            return new HttpMapping(methodValue, path);
        }
        return new HttpMapping(null, null);
    }

    private Optional<String> annotationPath(NodeWithAnnotations<?> node, String annotationName) {
        return node.getAnnotationByName(annotationName).flatMap(this::extractPath);
    }

    private Optional<String> extractPath(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            return Optional.of(stripQuotes(single.getMemberValue().toString()));
        }
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if ("value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString())) {
                    return Optional.of(stripQuotes(pair.getValue().toString()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> requestMappingMethod(MethodDeclaration method) {
        return method.getAnnotationByName("RequestMapping")
                .filter(NormalAnnotationExpr.class::isInstance)
                .map(NormalAnnotationExpr.class::cast)
                .flatMap(annotation -> annotation.getPairs().stream()
                        .filter(pair -> "method".equals(pair.getNameAsString()))
                        .map(pair -> pair.getValue().toString())
                        .map(value -> value.contains(".") ? value.substring(value.lastIndexOf('.') + 1) : value)
                        .map(value -> value.toUpperCase(Locale.ROOT))
                        .findFirst());
    }

    private String joinPaths(String prefix, String path) {
        if (prefix == null || prefix.isBlank()) {
            return path;
        }
        if (path == null || path.isBlank()) {
            return prefix;
        }
        if (prefix.endsWith("/") && path.startsWith("/")) {
            return prefix.substring(0, prefix.length() - 1) + path;
        }
        if (!prefix.endsWith("/") && !path.startsWith("/")) {
            return prefix + "/" + path;
        }
        return prefix + path;
    }

    private String stripQuotes(String value) {
        String trimmed = value.strip();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return stripQuotes(trimmed.substring(1, trimmed.length() - 1).strip());
        }
        return trimmed;
    }
}
