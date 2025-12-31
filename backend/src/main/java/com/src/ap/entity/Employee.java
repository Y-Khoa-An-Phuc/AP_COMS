package com.src.ap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "hire_dt", nullable = false)
    private LocalDate hireDt;

    @Column(name = "termination_dt")
    private LocalDate terminationDt;

    @Column(name = "citizen_id_card")
    private String citizenIdCard;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "passport")
    private String passport;

    @Column(name = "contract_id")
    private String contractId;

    private String phone;

    @Column(name = "work_status")
    private String workStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_id")
    private Occupation occupation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Employee supvEmployee;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_update_user")
    private String lastUpdateUser;

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