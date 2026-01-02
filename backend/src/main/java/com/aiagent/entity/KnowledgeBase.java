package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "knowledge_bases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "chunk_size")
    @Builder.Default
    private Integer chunkSize = 500;

    @Column(name = "chunk_overlap")
    @Builder.Default
    private Integer chunkOverlap = 50;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunking_strategy")
    @Builder.Default
    private ChunkingStrategy chunkingStrategy = ChunkingStrategy.FIXED_SIZE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "knowledgeBase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @ManyToMany(mappedBy = "knowledgeBases")
    @Builder.Default
    private Set<Agent> agents = new HashSet<>();

    public enum ChunkingStrategy {
        FIXED_SIZE, PARAGRAPH, SEMANTIC
    }
}
