package com.example.demo.repository;

import com.example.demo.entity.MPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MPayRepository extends JpaRepository<MPay, Long> {
    Optional<MPay> findBySid(Long sid);
}