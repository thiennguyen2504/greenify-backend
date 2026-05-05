package com.webdev.greenify.co2e.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "local_knowledge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private String type; // 'slang' or 'product'

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "embedding", columnDefinition = "vector(3072)")
    private float[] embedding;
}
