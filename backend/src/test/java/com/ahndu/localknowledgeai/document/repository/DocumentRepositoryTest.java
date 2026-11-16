
package com.ahndu.localknowledgeai.document.repository;

import com.ahndu.localknowledgeai.document.entity.Document;
import com.ahndu.localknowledgeai.document.entity.DocumentStatus;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndFindDocument() {

        // Given: Lombok Builder로 문서 객체 생성
        Document document = Document.builder()
                .originalFilename("test-document.pdf")
                .storedPath("/tmp/test-document.pdf")
                .fileType("PDF")
                .fileSize(1024L)
                .build();

        // UUID와 초기 상태 확인
        assertThat(document.getId()).isNotNull();
        assertThat(document.getStatus())
                .isEqualTo(DocumentStatus.UPLOADED);

        // When: 실제 PostgreSQL에 INSERT
        Document saved = documentRepository.saveAndFlush(document);

        UUID documentId = saved.getId();

        // JPA 영속성 컨텍스트 초기화
        entityManager.clear();

        // 실제 DB에서 SELECT
        Document found = documentRepository.findById(documentId)
                .orElseThrow();

        // Then: 저장된 데이터 검증
        assertThat(found.getId()).isEqualTo(documentId);

        assertThat(found.getOriginalFilename())
                .isEqualTo("test-document.pdf");

        assertThat(found.getStoredPath())
                .isEqualTo("/tmp/test-document.pdf");

        assertThat(found.getFileType())
                .isEqualTo("PDF");

        assertThat(found.getFileSize())
                .isEqualTo(1024L);

        assertThat(found.getStatus())
                .isEqualTo(DocumentStatus.UPLOADED);

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}
