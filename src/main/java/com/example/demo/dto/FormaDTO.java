package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FormaDTO {
    private Long id;
    private String solId;
    private Long payId;
    private String fio;
    private String nomerZayavleniya;
    private String istochnik;
    private String idSourType;
    private Long osnova;
    private Long idOsn;
    private String vidZayavleniya;
    private Long iin;
    private LocalDate dateBirth;
    private LocalDate dateObr;
    private LocalDate datePrivem;
    private String yazykZayavl;
    private String domTel;
    private String mobTel;
    private Boolean pribyl;
    private String stranaPrib;
    private String sposobViplaty;
    private LocalDate maketDateNazn;
    private LocalDate maketDateOkon;
    private BigDecimal maketNaznSumma;
    private String nResh;
    private LocalDate dResh;
    private String nomerDela;
    private Long maketId;
    private Long status;
    private LocalDateTime lastStatusDate;
    private String lastStatusUser;
    private String rejectReason;
    private String brid;
    private String inpBrid;
    private LocalDate dReg;
    private LocalDate estDate;
    private Long isOtkaz;
    private String conNum;
    private LocalDate conDat;
}