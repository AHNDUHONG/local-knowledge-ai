
package com.ahndu.localknowledgeai.document.chunk;

import com.ahndu.localknowledgeai.document.entity.Document;
import com.ahndu.localknowledgeai.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(
        autowireMode = TestConstructor.AutowireMode.ALL
)
class DocumentChunkJdbcRepositoryTest {

    private final DocumentRepository documentRepository;
    private final DocumentChunkJdbcRepository chunkRepository;
    private final JdbcTemplate jdbcTemplate;

    @Test
    void saveAndSearchOnlyCompletedDocuments() {

        // 1. 두 개의 문서 등록
        Document completedDocument = documentRepository.saveAndFlush(
                createDocument("completed.pdf")
        );

        Document uploadedDocument = documentRepository.saveAndFlush(
                createDocument("uploaded.pdf")
        );

        // 2. 첫 번째 문서만 COMPLETED 처리
        jdbcTemplate.update(
                "UPDATE document SET status = 'COMPLETED' WHERE id = ?",
                completedDocument.getId()
        );

        // 3. 테스트용 1024차원 벡터 생성
        float[] completedVector = createVector(1.0f, 0.0f);
        float[] uploadedVector = createVector(1.0f, 0.0f);

        // 4. 각 문서의 Chunk 저장
        UUID completedChunkId = chunkRepository.save(
                completedDocument.getId(),
                0,
                "Spring Boot는 Java 기반 프레임워크다.",
                completedVector,
                1
        );

        chunkRepository.save(
                uploadedDocument.getId(),
                0,
                "이 문서는 아직 처리되지 않았다.",
                uploadedVector,
                1
        );

        // 5. 의미 검색 실행
        List<ChunkSearchResult> results =
                chunkRepository.search(
                        createVector(1.0f, 0.0f),
                        10
                );

        // 6. COMPLETED 문서만 조회되는지 확인
        assertThat(results)
                .extracting(ChunkSearchResult::chunkId)
                .contains(completedChunkId);

        assertThat(results)
                .extracting(ChunkSearchResult::documentId)
                .doesNotContain(uploadedDocument.getId());

        ChunkSearchResult match = results.stream()
                .filter(result ->
                        result.chunkId().equals(completedChunkId))
                .findFirst()
                .orElseThrow();

        assertThat(match.cosineDistance())
                .isCloseTo(
                        0.0,
                        org.assertj.core.data.Offset.offset(0.0001)
                );
    }

    private Document createDocument(String filename) {
        return Document.builder()
                .originalFilename(filename)
                .storedPath("/tmp/" + UUID.randomUUID())
                .fileType("PDF")
                .fileSize(1024L)
                .build();
    }

    private float[] createVector(float first, float second) {
        float[] vector = new float[1024];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }
}
