package com.example.demo.service;

import com.example.demo.dto.MacetDTO;
import com.example.demo.entity.MPay;
import com.example.demo.entity.MSol;
import com.example.demo.entity.ZHistory;
import com.example.demo.repository.MPayRepository;
import com.example.demo.repository.MSolRepository;
import com.example.demo.repository.ZDocRepository;
import com.example.demo.repository.ZHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MacetService {

    @Autowired private MSolRepository solRepo;
    @Autowired private MPayRepository payRepo;
    @Autowired private ZDocRepository zDocRepo;
    @Autowired private ZHistoryRepository historyRepo;

    @Transactional
    public MacetDTO save(MacetDTO dto) {

        LocalDate today = LocalDate.now();
        boolean isRec = Boolean.TRUE.equals(dto.getIsPereschet());

        Long sharedId = dto.getZdocId();
        if (sharedId == null) {
            throw new RuntimeException("zdocId обязателен");
        }

        long payId = (System.currentTimeMillis() % 1_000_000_000L) + 300_000_000L;

        // Шаг 1: m_sol
        Optional<MSol> existingSol = solRepo.findById(sharedId);
        MSol sol;
        String oldSum = "-";

        if (existingSol.isPresent()) {
            sol = existingSol.get();
            oldSum = sol.getNsum() != null ? sol.getNsum().toPlainString() : "-";
            // Обновляем z_numb чтобы buildDto находил через findByZNumb
            if (dto.getNomerZayavleniya() != null) {
                sol.setZNumb(dto.getNomerZayavleniya());
            }
        } else {
            sol = new MSol();
            sol.setId(sharedId);
            sol.setPid(sharedId);
            sol.setOrg("1");
            sol.setZNumb(dto.getNomerZayavleniya());
            sol.setZDate(LocalDateTime.now());
            sol.setBrid(dto.getBrid() != null ? dto.getBrid() : "0000");
            sol.setSicid(dto.getSicid() != null ? dto.getSicid() : sharedId);
            sol.setNumb("E" + (dto.getBrid() != null ? dto.getBrid() : "0000") + sharedId);
            sol.setEmpId(1L);
            sol.setInDate(LocalDateTime.now());
        }
        sol.setSt(isRec ? 12L : 1L);
        sol.setNsum(dto.getNaznSumma());
        sol.setNResh("");
        sol.setDResh(dto.getDateNazn() != null ? dto.getDateNazn() : today);
        sol.setMpay(payId);
        sol.setMid(sharedId);
        solRepo.save(sol);

        // Шаг 2: m_pay
        Optional<MPay> existingPay = payRepo.findBySid(sharedId);
        MPay pay;
        if (existingPay.isPresent()) {
            pay = existingPay.get();
        } else {
            pay = new MPay();
            pay.setId(payId);
            pay.setSid(sharedId);
        }
        pay.setNsum(dto.getNaznSumma());
        pay.setPc(dto.getSposobViplaty() != null ? dto.getSposobViplaty() : "");
        pay.setDNaz(dto.getDateNazn() != null ? dto.getDateNazn() : today);
        pay.setStopdate(dto.getDateOkon());
        pay.setStatus(1L);
        payRepo.save(pay);

        // Шаг 3: z_doc
        zDocRepo.findById(sharedId).ifPresent(zd -> {
            zd.setIdsol(sharedId);
            zd.setIdTip(isRec ? "REC" : "NEW");
            zd.setEstDate(dto.getDateOkon());
            zDocRepo.save(zd);
        });

        // Шаг 4: z_history — записываем действие
        ZHistory hist = new ZHistory();
        hist.setZdocId(sharedId);
        hist.setIin(dto.getIin());
        hist.setDat(LocalDateTime.now());
        hist.setFieldName("NSUM");
        hist.setOldValue(oldSum);
        hist.setNewValue(dto.getNaznSumma() != null ? dto.getNaznSumma().toPlainString() : "-");
        hist.setDeystvie(isRec ? "Перерасчёт" : "Новое назначение");
        hist.setUsr("EMAKET");
        historyRepo.save(hist);

        return dto;
    }
}