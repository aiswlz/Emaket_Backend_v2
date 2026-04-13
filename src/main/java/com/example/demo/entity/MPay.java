package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "m_pay", schema = "em5")
@Data
public class MPay {
    @Id @Column(name = "id") private Long id;
    @Column(name = "sid") private Long sid;
    @Column(name = "nsum") private BigDecimal nsum;
    @Column(name = "soliray_id") private Long solirayId;
    @Column(name = "id_guard") private Long idGuard;
    @Column(name = "pc") private String pc;
    @Column(name = "d_naz") private LocalDate dNaz;
    @Column(name = "stopdate") private LocalDate stopdate;
    @Column(name = "closedate") private LocalDate closedate;
    @Column(name = "closeact") private Long closeact;
    @Column(name = "status") private Long status;
    @Column(name = "prior_id") private Long priorId;
}