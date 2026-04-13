package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ElZayavkaDTO {
    private Long id;
    private LocalDate data;
    private String nomerZayavleniya;
    private String otdelenie;
    private Long iin;
    private String fio;
    private LocalDate dataRozhdeniya;
    private Long status;
    private Long osnova;
    private String istochnik;
    private String tipistochnika;
    private String kommentariy;
}