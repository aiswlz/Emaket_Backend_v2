package com.example.demo.entity.reports;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "report_gfss_view", schema = "em5")
public class ReportGfssView {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "action_type")
    private Integer actionType;

    @Column(name = "name_rus")
    private String nameRus;

    @Column(name = "name_kaz")
    private String nameKaz;

    @Column(name = "rep_id")
    private Long repId;

    @Column(name = "cmd")
    private String cmd;

    @Column(name = "async")
    private String async;

    @Column(name = "maskid")
    private Long maskId;

    @Column(name = "lev1")
    private String lev1;

    @Column(name = "lev1_kz")
    private String lev1Kz;

    @Column(name = "ord")
    private Integer ord;
}