package com.src.ap.repository;

import com.src.ap.entity.Occupation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OccupationRepository extends JpaRepository<Occupation, Long>, JpaSpecificationExecutor<Occupation> {
    Optional<Occupation> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT o FROM Occupation o WHERE " +
           "LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Occupation> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Fetches distinct occupation names for filter values.
     * Used by ENUM-type filters to populate dropdown options.
     *
     * @return list of distinct occupation names, sorted alphabetically
     */
    @Query("SELECT DISTINCT o.name FROM Occupation o WHERE o.name IS NOT NULL ORDER BY o.name")
    List<String> findDistinctNames();
}