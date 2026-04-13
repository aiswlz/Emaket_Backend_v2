package com.example.demo.repository;

import com.example.demo.entity.MEg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MEgRepository extends JpaRepository<MEg, Long> {
    Optional<MEg> findByIin(Long iin);
}