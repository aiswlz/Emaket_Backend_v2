package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "z_history", schema = "em5")
@Data
public class ZHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "z_history_gen")
    @SequenceGenerator(
            name = "z_history_gen",
            sequenceName = "em5.z_history_id_seq",  // ← z_history_id_seq
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "zdoc_id")
    private Long zdocId;

    @Column(name = "iin")
    private Long iin;

    @Column(name = "dat")
    private LocalDateTime dat;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "deystvie")
    private String deystvie;

    @Column(name = "usr")
    private String usr;
}