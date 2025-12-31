package com.src.ap.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "occupations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Occupation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, unique = true)
    private String name;

    @Nationalized
    private String description;

    @OneToMany(mappedBy = "occupation", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
