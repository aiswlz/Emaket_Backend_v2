package com.example.demo.service;

import com.example.demo.dto.ZayavlenieDTO;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ZayavlenieService {

    @Autowired private MEgRepository egRepo;
    @Autowired private MSolRepository solRepo;
    @Autowired private MPayRepository payRepo;
    @Autowired private ZDocRepository zDocRepo;

    public List<ZayavlenieDTO> getAll() {
        List<ZayavlenieDTO> result = new ArrayList<>();

        egRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getDReg() == null) return 1;
                    if (b.getDReg() == null) return -1;
                    return b.getDReg().compareTo(a.getDReg());
                })
                .forEach(eg -> {
                    // Для каждого клиента ищем ВСЕ его z_doc (одно основание = одна запись)
                    List<com.example.demo.entity.ZDoc> zdList = zDocRepo.findAllByIdEg(eg.getId());
                    // Fallback: старая схема где z_doc.id = m_eg.id
                    if (zdList.isEmpty()) {
                        zDocRepo.findById(eg.getId()).ifPresent(zdList::add);
                    }

                    for (com.example.demo.entity.ZDoc z : zdList) {
                        ZayavlenieDTO dto = new ZayavlenieDTO();
                        dto.setId(z.getId()); // ID заявления (z_doc), не клиента
                        dto.setIin(eg.getIin());
                        dto.setFio(eg.getLn() + " " + eg.getFn() + " " +
                                (eg.getMn() != null ? eg.getMn() : ""));
                        dto.setDateBirth(eg.getBd());
                        dto.setDateReg(eg.getDReg() != null ? eg.getDReg().toLocalDate() : null);
                        dto.setOsnova(z.getIdOsn()); // основание из z_doc, не из m_eg
                        dto.setNomer(z.getNum());
                        dto.setDateObr(z.getDInp());
                        dto.setKodOtd(z.getBrid());
                        dto.setTipZayav(z.getIdTip());
                        dto.setTipIstochnikaZayav(z.getIdSourType());

                        solRepo.findByZNumb(z.getNum())
                                .or(() -> solRepo.findById(z.getId()))
                                .ifPresent(sol -> {
                                    dto.setNomerDela(sol.getNumb());
                                    dto.setSpecialist(sol.getEmpId());
                                    dto.setDateResh(sol.getDResh());

                                    payRepo.findBySid(sol.getId()).ifPresent(pay -> {
                                        dto.setVidViplaty(pay.getPc());
                                        dto.setRazmer(pay.getNsum());
                                        dto.setDateNazn(pay.getDNaz());
                                    });
                                });

                        result.add(dto);
                    }
                });

        return result;
    }
}