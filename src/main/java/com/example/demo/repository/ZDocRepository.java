package com.example.demo.repository;
import java.util.Optional;

import com.example.demo.entity.ZDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZDocRepository extends JpaRepository<ZDoc, Long> {
    Optional<ZDoc> findByNum(String num);
}