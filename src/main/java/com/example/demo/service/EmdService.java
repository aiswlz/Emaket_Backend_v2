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
                    Long egId = eg.getId();

                    // Ищем z_doc по id_eg_ (новая схема), затем по sicid, fallback по id
                    java.util.List<com.example.demo.entity.ZDoc> zdList =
                            zDocRepo.findAllByIdEg(egId);

                    if (zdList.isEmpty()) {
                        Long sicid = eg.getIdAcc() != null ? eg.getIdAcc() : egId;
                        zdList = zDocRepo.findAllBySicid(sicid);
                    }
                    if (zdList.isEmpty()) {
                        zDocRepo.findById(egId).ifPresent(zdList::add);
                    }

                    // Если z_doc не найден — пропускаем запись (нет макета)
                    if (zdList.isEmpty()) return null;

                    EmdDTO dto = new EmdDTO();
                    dto.setId(eg.getId());
                    dto.setDateObr(eg.getDat() != null ? eg.getDat().toLocalDate() : null);
                    dto.setKodOtd(eg.getBrid());
                    dto.setIin(eg.getIin());
                    dto.setFio(eg.getLn() + " " + eg.getFn() + " " +
                            (eg.getMn() != null ? eg.getMn() : ""));
                    dto.setDateBirth(eg.getBd());
                    dto.setIstochnik(eg.getIdSour());
                    dto.setVidviplat(eg.getIdOsn());

                    com.example.demo.entity.ZDoc z = zdList.get(0);
                    dto.setZdocId(z.getId());
                    dto.setSrokOkazaniya(z.getEstDate());

                    // Ищем m_sol по z_numb, fallback по id
                    solRepo.findByZNumb(z.getNum())
                            .or(() -> solRepo.findById(z.getId()))
                            .ifPresent(sol -> {
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

                    return dto;
                })
                // Фильтруем null — записи без z_doc не показываем
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}