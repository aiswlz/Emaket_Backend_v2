package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "m_doc", schema = "em5")
@Data
public class MDoc {
    @Id @Column(name = "id") private Long id;
    @Column(name = "id_eg") private Long idEg;
    @Column(name = "id_msg") private Long idMsg;
    @Column(name = "id_dt") private Long idDt;
    @Column(name = "doctypecode") private String doctypecode;
    @Column(name = "doccopytype") private String doccopytype;
    @Column(name = "docmimetype") private String docmimetype;
    @Column(name = "docname") private String docname;
    @Column(name = "dat") private LocalDate dat;
    @Column(name = "url") private String url;
    @Column(name = "dt") private String dt;
    @Column(name = "err") private String err;
    @Column(name = "st") private Long st;
    @Column(name = "adeq") private Long adeq;
    @Column(name = "id_outmsg") private Long idOutmsg;
    @Column(name = "id_who") private Long idWho;
    @Column(name = "id_own") private Long idOwn;
    @Column(name = "copytip") private String copytip;
    @Column(name = "iin_own") private Long iinOwn;
    @Column(name = "xres") private Long xres;
    @Column(name = "trycnt") private Long trycnt;
    @Column(name = "docinfo") private String docinfo;
    @Column(name = "is_import") private Long isImport;
}