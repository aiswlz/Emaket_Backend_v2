package com.example.demo.repository;

import com.example.demo.entity.MSol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MSolRepository extends JpaRepository<MSol, Long> {

    @Query("SELECT m FROM MSol m WHERE m.zNumb = :zNumb")
    Optional<MSol> findByZNumb(@Param("zNumb") String zNumb);
}