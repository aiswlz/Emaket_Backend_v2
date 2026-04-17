package com.example.demo.repository.reports;

import com.example.demo.entity.reports.ReportGfssView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportGfssRepository
        extends JpaRepository<ReportGfssView, Long> {
}