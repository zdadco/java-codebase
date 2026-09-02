package ru.zdadco.indexer.core.search;

import java.util.Map;

public record SearchHit(
        String id,
        String content,
        Double score,
        Map<String, Object> metadata
) {
}
