package com.example.demo.entity.reports;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "order_for_report", schema = "em5")
public class OrderForReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_report")
    private Long idReport;

    @Column(name = "params")
    private String params;

    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "status")
    private Short status;

    @Column(name = "beg_date")
    private LocalDate begDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "dat")
    private LocalDateTime dat;

    @Column(name = "job")
    private Long job;

    @Column(name = "report")
    private String report;

    @Column(name = "err")
    private String err;
}