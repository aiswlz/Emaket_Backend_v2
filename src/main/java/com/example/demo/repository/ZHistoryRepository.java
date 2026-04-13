package com.example.demo.repository;

import com.example.demo.entity.ZHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ZHistoryRepository extends JpaRepository<ZHistory, Long> {
    List<ZHistory> findByZdocIdOrderByDatDesc(Long zdocId);
    List<ZHistory> findByIinOrderByDatDesc(Long iin);
}