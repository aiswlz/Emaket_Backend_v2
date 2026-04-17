package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import com.example.demo.entity.ZDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZDocRepository extends JpaRepository<ZDoc, Long> {
    // Find all z_doc records for a given client (new schema: id_eg_ field)
    List<ZDoc> findAllByIdEg(Long egId);
    Optional<ZDoc> findFirstByIdEg(Long egId);

    // Fallback for old data where z_doc was linked via sicid
    List<ZDoc> findAllBySicid(Long sicid);
}