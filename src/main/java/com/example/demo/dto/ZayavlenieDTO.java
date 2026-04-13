package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ZayavlenieDTO {
    private Long id;
    private String nomer;
    private LocalDate dateReg;
    private LocalDate dateObr;
    private String kodOtd;
    private String nomerDela;
    private Long iin;
    private String fio;
    private LocalDate dateBirth;
    private Long osnova;
    private String vidViplaty;
    private String tipZayav;
    private String tipIstochnikaZayav;
    private Long specialist;
    private LocalDate dateResh;
    private BigDecimal razmer;
    private LocalDate dateNazn;
}