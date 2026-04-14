package com.example.demo.service;

import com.example.demo.dto.ZayavlenieDTO;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ZayavlenieService {

    @Autowired private MEgRepository egRepo;
    @Autowired private MSolRepository solRepo;
    @Autowired private MPayRepository payRepo;
    @Autowired private ZDocRepository zDocRepo;

    public List<ZayavlenieDTO> getAll() {
        return egRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getDReg() == null) return 1;
                    if (b.getDReg() == null) return -1;
                    return b.getDReg().compareTo(a.getDReg());
                })
                .map(eg -> {
                    ZayavlenieDTO dto = new ZayavlenieDTO();
                    dto.setId(eg.getId());
                    dto.setIin(eg.getIin());
                    dto.setFio(eg.getLn() + " " + eg.getFn() + " " +
                            (eg.getMn() != null ? eg.getMn() : ""));
                    dto.setDateBirth(eg.getBd());
                    dto.setDateReg(eg.getDReg() != null ? eg.getDReg().toLocalDate() : null);
                    dto.setOsnova(eg.getIdOsn());

                    solRepo.findById(eg.getId()).ifPresent(sol -> {
                        dto.setNomerDela(sol.getNumb());
                        dto.setSpecialist(sol.getEmpId());
                        dto.setDateResh(sol.getDResh());

                        payRepo.findBySid(sol.getId()).ifPresent(pay -> {
                            dto.setVidViplaty(pay.getPc());
                            dto.setRazmer(pay.getNsum());
                            dto.setDateNazn(pay.getDNaz());
                        });
                    });

                    zDocRepo.findById(eg.getId()).ifPresent(z -> {
                        dto.setNomer(z.getNum());
                        dto.setDateObr(z.getDInp());
                        dto.setKodOtd(z.getBrid());
                        dto.setTipZayav(z.getIdTip());
                        dto.setTipIstochnikaZayav(z.getIdSourType());
                    });

                    return dto;
                }).collect(Collectors.toList());
    }
}