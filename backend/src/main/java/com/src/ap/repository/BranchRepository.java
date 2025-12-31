package com.src.ap.repository;

import com.src.ap.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT b FROM Branch b WHERE " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.country) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Branch> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}