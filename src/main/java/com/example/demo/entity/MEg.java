package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "m_eg", schema = "em5")
@Data
public class MEg {
    @Id @Column(name = "id") private Long id;
    @Column(name = "id_msg") private Long idMsg;
    @Column(name = "id_st") private Long idSt;
    @Column(name = "dat") private LocalDateTime dat;
    @Column(name = "brid") private String brid;
    @Column(name = "pc") private String pc;
    @Column(name = "iin") private Long iin;
    @Column(name = "ln") private String ln;
    @Column(name = "fn") private String fn;
    @Column(name = "mn") private String mn;
    @Column(name = "bd") private LocalDate bd;
    @Column(name = "comm") private String comm;
    @Column(name = "id_sour") private String idSour;
    @Column(name = "znum") private String znum;
    @Column(name = "id_acc") private Long idAcc;
    @Column(name = "d_reg") private LocalDateTime dReg;
    @Column(name = "d_inp") private LocalDateTime dInp;
    @Column(name = "d_est") private LocalDate dEst;
    @Column(name = "lang") private String lang;
    @Column(name = "id_osn") private Long idOsn;
    @Column(name = "id_sour_type") private String idSourType;
    @Column(name = "con_brid") private String conBrid;
    @Column(name = "mobile_phone") private String mobilePhone;
    @Column(name = "mobile_source") private String mobileSource;
}