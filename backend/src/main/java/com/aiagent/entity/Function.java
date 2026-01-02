package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "functions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Function extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "parameters_schema", columnDefinition = "TEXT")
    private String parametersSchema;

    @Column(name = "return_type")
    private String returnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "implementation_type", nullable = false)
    private ImplementationType implementationType;

    @Column(name = "implementation_config", columnDefinition = "TEXT")
    private String implementationConfig;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(mappedBy = "functions")
    @Builder.Default
    private Set<Agent> agents = new HashSet<>();

    public enum ImplementationType {
        HTTP_API, CODE, TEMPLATE
    }
}
