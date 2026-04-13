package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "z_doc", schema = "em5")
@Data
public class ZDoc {
    @Id @Column(name = "id") private Long id;
    @Column(name = "num") private String num;
    @Column(name = "brid") private String brid;
    @Column(name = "sicid") private Long sicid;
    @Column(name = "d_inp") private LocalDate dInp;
    @Column(name = "d_inp_doc") private LocalDate dInpDoc;
    @Column(name = "d_reg") private LocalDate dReg;
    @Column(name = "id_sour") private String idSour;
    @Column(name = "id_sour_type") private String idSourType;
    @Column(name = "id_osn") private Long idOsn;
    @Column(name = "con_num") private String conNum;
    @Column(name = "con_dat") private LocalDate conDat;
    @Column(name = "id_ext_cntr") private Long idExtCntr;
    @Column(name = "id_ext_br") private String idExtBr;
    @Column(name = "id_tip") private String idTip;
    @Column(name = "idsol") private Long idsol;
    @Column(name = "is_otkaz") private Long isOtkaz;
    @Column(name = "id_emp") private Long idEmp;
    @Column(name = "dat") private LocalDate dat;
    @Column(name = "id_eg_") private Long idEg;
    @Column(name = "id_iz") private Long idIz;
    @Column(name = "est_date") private LocalDate estDate;
    @Column(name = "est_change") private Long estChange;
    @Column(name = "doclang") private String doclang;
    @Column(name = "idperson") private Long idperson;
    @Column(name = "rfrc_id") private Long rfrcId;
    @Column(name = "idreason") private Long idreason;
    @Column(name = "home_phone") private String homePhone;
    @Column(name = "inp_brid") private String inpBrid;
    @Column(name = "usr") private String usr;
    @Column(name = "mobile_phone") private String mobilePhone;
    @Column(name = "mobile_source") private String mobileSource;
    @Column(name = "z_id_") private Long zId;
}