
package com.ahndu.localknowledgeai.document.repository;

import com.ahndu.localknowledgeai.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {
}
