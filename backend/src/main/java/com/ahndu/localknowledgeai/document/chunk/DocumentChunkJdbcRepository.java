
package com.ahndu.localknowledgeai.document.chunk;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentChunkJdbcRepository {

    private static final int EMBEDDING_DIMENSIONS = 1024;

    private final JdbcTemplate jdbcTemplate;

    public UUID save(
            UUID documentId,
            int chunkIndex,
            String content,
            float[] embedding,
            Integer pageNumber
    ) {
        validateEmbedding(embedding);

        if (documentId == null
                || chunkIndex < 0
                || content == null
                || content.isBlank()
                || (pageNumber != null && pageNumber < 1)) {
            throw new IllegalArgumentException(
                    "Invalid document chunk data"
            );
        }

        UUID chunkId = UUID.randomUUID();

        String sql = """
                INSERT INTO document_chunk (
                    id,
                    document_id,
                    chunk_index,
                    content,
                    embedding,
                    page_number
                )
                VALUES (?, ?, ?, ?, ?::vector, ?)
                """;

        jdbcTemplate.update(
                sql,
                chunkId,
                documentId,
                chunkIndex,
                content,
                toVectorLiteral(embedding),
                pageNumber
        );

        return chunkId;
    }

    public List<ChunkSearchResult> search(
            float[] queryEmbedding,
            int limit
    ) {
        validateEmbedding(queryEmbedding);

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 100"
            );
        }

        String sql = """
                SELECT
                    dc.id AS chunk_id,
                    dc.document_id,
                    dc.chunk_index,
                    dc.content,
                    dc.page_number,
                    dc.embedding <=> ?::vector
                        AS cosine_distance
                FROM document_chunk dc
                INNER JOIN document d
                    ON d.id = dc.document_id
                WHERE d.status = 'COMPLETED'
                ORDER BY dc.embedding <=> ?::vector
                LIMIT ?
                """;

        String vector = toVectorLiteral(queryEmbedding);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChunkSearchResult(
                        rs.getObject("chunk_id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getObject("page_number", Integer.class),
                        rs.getDouble("cosine_distance")
                ),
                vector,
                vector,
                limit
        );
    }

    private void validateEmbedding(float[] embedding) {
        if (embedding == null
                || embedding.length != EMBEDDING_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Embedding must contain 1024 dimensions"
            );
        }

        boolean nonZero = false;

        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Embedding contains a non-finite value"
                );
            }

            if (value != 0.0f) {
                nonZero = true;
            }
        }

        if (!nonZero) {
            throw new IllegalArgumentException(
                    "Embedding must not be a zero vector"
            );
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(embedding[i]);
        }

        return result.append("]").toString();
    }
}
