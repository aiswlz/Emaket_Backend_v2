package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmdDTO {
    private Long id;
    private LocalDate dateObr;
    private LocalDate dateStatus;
    private Long status;
    private String kodOtd;
    private Long iin;
    private String fio;
    private LocalDate dateBirth;
    private String istochnik;
    private LocalDate dateNazn;
    private LocalDate dateOkon;
    private BigDecimal naznRazmer;
    private LocalDate srokOkazaniya;
    private BigDecimal viplata;
    private Long vidviplat;
    private Long specialist;
}