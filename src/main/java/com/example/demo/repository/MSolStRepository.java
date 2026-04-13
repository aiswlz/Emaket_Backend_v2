package com.example.demo.repository;

import com.example.demo.entity.MSolSt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MSolStRepository extends JpaRepository<MSolSt, Long> {
    Optional<MSolSt> findTopBySidOrderByDatDesc(Long sid);
}