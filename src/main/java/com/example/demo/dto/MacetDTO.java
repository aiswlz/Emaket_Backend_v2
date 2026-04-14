package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MacetDTO {
    private Long       zdocId;          // id из z_doc (FK)
    private String     nomerZayavleniya; // номер заявления = z_doc.num
    private String     brid;
    private Long       sicid;
    private Long       idOsn;           // основание (103, 104...)
    private BigDecimal naznSumma;       // назначенная сумма → m_sol.nsum + m_pay.nsum
    private String     sposobViplaty;   // способ выплаты → m_pay.pc
    private LocalDate  dateNazn;        // дата назначения → m_pay.d_naz
    private LocalDate  dateOkon;        // дата окончания → m_pay.stopdate
    private Long       iin;
    private Boolean    isPereschet;     // перерасчёт → z_doc.id_tip = "REC"
}