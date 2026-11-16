package com.ahndu.localknowledgeai.document.chunk;

import java.util.UUID;

public record ChunkSearchResult(
        UUID chunkId,
        UUID documentId,
        int chunkIndex,
        String content,
        Integer pageNumber,
        double cosineDistance
) {
}