package com.example.demo.repository;

import com.example.demo.entity.MDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MDocRepository extends JpaRepository<MDoc, Long> {
    List<MDoc> findByIdEg(Long idEg);
}