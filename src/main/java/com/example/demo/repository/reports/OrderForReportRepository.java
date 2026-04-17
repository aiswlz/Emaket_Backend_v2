package com.example.demo.repository.reports;

import com.example.demo.entity.reports.OrderForReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderForReportRepository extends JpaRepository<OrderForReport, Long> {
}