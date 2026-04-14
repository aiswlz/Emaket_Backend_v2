package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "m_sol", schema = "em5")
@Data
public class MSol {
    @Id @Column(name = "id") private Long id;
    @Column(name = "st") private Long st;
    @Column(name = "pid") private Long pid;
    @Column(name = "id_file_") private Long idFile;
    @Column(name = "nsum") private BigDecimal nsum;
    @Column(name = "org") private String org;
    @Column(name = "z_numb") private String zNumb;
    @Column(name = "z_date") private LocalDateTime zDate;
    @Column(name = "n_resh") private String nResh;
    @Column(name = "d_resh") private LocalDate dResh;
    @Column(name = "mpay") private Long mpay;
    @Column(name = "mid") private Long mid;
    @Column(name = "izid") private Long izid;
    @Column(name = "brid") private String brid;
    @Column(name = "sicid") private Long sicid;
    @Column(name = "in_date") private LocalDateTime inDate;
    @Column(name = "hid") private Long hid;
    @Column(name = "z_id_") private Long zId;
    @Column(name = "numb") private String numb;
    @Column(name = "emp_id") private Long empId;
}