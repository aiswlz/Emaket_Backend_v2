package com.example.demo.service;

import com.example.demo.dto.EmdDTO;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmdService {

    @Autowired private MEgRepository egRepo;
    @Autowired private MSolRepository solRepo;
    @Autowired private MPayRepository payRepo;
    @Autowired private ZDocRepository zDocRepo;

    public List<EmdDTO> getAll() {
        return egRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getDat() == null) return 1;
                    if (b.getDat() == null) return -1;
                    return b.getDat().compareTo(a.getDat());
                })
                .map(eg -> {
            EmdDTO dto = new EmdDTO();
            dto.setId(eg.getId());
            dto.setDateObr(eg.getDat());
            dto.setKodOtd(eg.getBrid());
            dto.setIin(eg.getIin());
            dto.setFio(eg.getLn() + " " + eg.getFn() + " " +
                    (eg.getMn() != null ? eg.getMn() : ""));
            dto.setDateBirth(eg.getBd());
            dto.setIstochnik(eg.getIdSour());
            dto.setVidviplat(eg.getIdOsn());

            solRepo.findById(eg.getId()).ifPresent(sol -> {
                dto.setStatus(sol.getSt());
                dto.setDateStatus(sol.getDResh());
                dto.setNaznRazmer(sol.getNsum());
                dto.setSpecialist(sol.getEmpId());

                payRepo.findBySid(sol.getId()).ifPresent(pay -> {
                    dto.setDateNazn(pay.getDNaz());
                    dto.setDateOkon(pay.getStopdate());
                    dto.setViplata(pay.getNsum());
                });
            });

            zDocRepo.findById(eg.getId()).ifPresent(z ->
                    dto.setSrokOkazaniya(z.getEstDate())
            );

            return dto;
        }).collect(Collectors.toList());
    }
}