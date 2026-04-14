package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "m_sol_st", schema = "em5")
@Data
public class MSolSt {
    @Id @Column(name = "id") private Long id;
    @Column(name = "sid") private Long sid;
    @Column(name = "st1") private Long st1;
    @Column(name = "st2") private Long st2;
    @Column(name = "dat") private LocalDateTime dat;
    @Column(name = "empid") private Long empid;
    @Column(name = "brid") private String brid;
    @Column(name = "usr") private String usr;
    @Column(name = "ip") private String ip;
    @Column(name = "host") private String host;
    @Column(name = "husr") private String husr;
    @Column(name = "ret_id") private Long retId;
    @Column(name = "ret_txt") private String retTxt;
    @Column(name = "backid") private Long backid;
    @Column(name = "iscon") private Long iscon;
    @Column(name = "isapp") private Long isapp;
    @Column(name = "s_brid") private String sBrid;
    @Column(name = "p_pc") private String pPc;
    @Column(name = "s_brid_old") private String sBridOld;
    @Column(name = "p_pc_old") private String pPcOld;
    @Column(name = "s_st") private String sSt;
}