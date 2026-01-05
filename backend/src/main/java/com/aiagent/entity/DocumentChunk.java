package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_document_id", columnList = "document_id"),
    @Index(name = "idx_chunk_knowledge_base_id", columnList = "knowledge_base_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false)
    private String chunkId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;
}
