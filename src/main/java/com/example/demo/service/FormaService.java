package com.example.demo.service;

import com.example.demo.dto.FormaDTO;
import com.example.demo.entity.MEg;
import com.example.demo.entity.ZDoc;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FormaService {

    @Autowired private ZDocRepository zDocRepo;
    @Autowired private MEgRepository egRepo;
    @Autowired private MSolRepository solRepo;
    @Autowired private MPayRepository payRepo;
    @Autowired private MSolStRepository solStRepo;

    public Optional<FormaDTO> getById(Long id) {
        return zDocRepo.findById(id).map(zd -> {
            MEg eg = egRepo.findById(id).orElse(null);
            return buildDto(eg, zd);
        });
    }

    public Optional<FormaDTO> getByIin(Long iin) {
        return egRepo.findFirstByIin(iin).flatMap(eg -> {
            if (eg.getZnum() == null) return Optional.empty();
            return zDocRepo.findByNum(eg.getZnum())
                    .map(zd -> buildDto(eg, zd));
        });
    }

    public List<FormaDTO> getAllByIin(Long iin) {
        List<MEg> egList = egRepo.findAllByIin(iin);
        List<FormaDTO> result = new ArrayList<>();

        for (MEg eg : egList) {
            // m_eg.ID = z_doc.ID — прямая связь через общий первичный ключ
            zDocRepo.findById(eg.getId()).ifPresent(zd ->
                    result.add(buildDto(eg, zd))
            );
        }

        return result;
    }

    public Optional<FormaDTO> getClientByIin(Long iin) {
        return egRepo.findFirstByIin(iin).map(eg -> {
            FormaDTO dto = new FormaDTO();
            dto.setIin(eg.getIin());
            String fio = (eg.getLn() != null ? eg.getLn() : "") + " "
                    + (eg.getFn() != null ? eg.getFn() : "")
                    + (eg.getMn() != null ? " " + eg.getMn() : "");
            dto.setFio(fio.trim());
            dto.setDateBirth(eg.getBd());
            dto.setBrid(eg.getBrid());
            dto.setMobTel(eg.getMobilePhone());
            return dto;
        });
    }

    public FormaDTO create(FormaDTO dto) {

        // m_eg и z_doc имеют ОДИНАКОВЫЙ ID — ищем клиента сначала
        MEg eg = dto.getIin() != null
                ? egRepo.findFirstByIin(dto.getIin()).orElse(null)
                : null;

        if (eg == null) {
            throw new RuntimeException("Клиент с ИИН " + dto.getIin() + " не найден в m_eg");
        }

        LocalDate today = LocalDate.now();
        String nomerZayavl = dto.getNomerZayavleniya() != null
                ? dto.getNomerZayavleniya()
                : String.valueOf(eg.getId());
        String brid  = eg.getBrid() != null ? eg.getBrid() : "0000";
        Long   sicid = eg.getIdAcc() != null ? eg.getIdAcc() : eg.getId();

        // z_doc.ID = m_eg.ID  (общий первичный ключ)
        ZDoc zd = new ZDoc();
        zd.setId(eg.getId());
        zd.setNum(nomerZayavl);
        zd.setConNum(nomerZayavl);
        zd.setConDat(LocalDateTime.now());
        zd.setDInp(dto.getDateObr() != null ? dto.getDateObr() : today);
        zd.setDInpDoc(dto.getDatePrivem() != null ? dto.getDatePrivem() : today);
        zd.setDReg(today);
        zd.setDat(LocalDateTime.now());
        zd.setIdTip("NEW");
        zd.setDoclang("ru");
        zd.setIsOtkaz(0L);
        zd.setEstDate(today.plusMonths(2));
        zd.setEstChange(1L);
        zd.setBrid(brid);
        zd.setSicid(sicid);
        zd.setIdOsn(eg.getIdOsn() != null ? eg.getIdOsn() : 103L);
        zd.setIdSour(eg.getIdSour() != null ? eg.getIdSour() : "WEB");
        zd.setIdSourType(eg.getIdSourType() != null ? eg.getIdSourType() : "PRA");
        zd.setMobilePhone(eg.getMobilePhone() != null ? eg.getMobilePhone() : dto.getMobTel());
        zd.setMobileSource(eg.getMobileSource() != null ? eg.getMobileSource() : "WEB");
        zd.setHomePhone(dto.getDomTel());
        zd.setIdEmp(1L);
        zd.setUsr("EMAKET");
        zDocRepo.save(zd);

        // Обновляем m_eg.znum = z_doc.num для связи через номер
        eg.setZnum(nomerZayavl);
        egRepo.save(eg);

        return buildDto(eg, zd);
    }

    public Optional<FormaDTO> update(Long id, FormaDTO dto) {
        return zDocRepo.findById(id).map(zd -> {

            if (dto.getDateObr() != null)    zd.setDInp(dto.getDateObr());
            if (dto.getDatePrivem() != null) zd.setDInpDoc(dto.getDatePrivem());
            if (dto.getYazykZayavl() != null) zd.setDoclang("ru");
            if (dto.getDomTel() != null)     zd.setHomePhone(dto.getDomTel());
            if (dto.getMobTel() != null)     zd.setMobilePhone(dto.getMobTel());

            zDocRepo.save(zd);

            MEg eg = null;
            if (dto.getIin() != null) {
                Optional<MEg> egOpt = egRepo.findFirstByIin(dto.getIin());
                if (egOpt.isPresent()) {
                    MEg egEntity = egOpt.get();
                    if (dto.getMobTel() != null) egEntity.setMobilePhone(dto.getMobTel());
                    egRepo.save(egEntity);
                    eg = egEntity;
                }
            }

            return buildDto(eg, zd);
        });
    }

    private FormaDTO buildDto(MEg eg, ZDoc zd) {
        FormaDTO dto = new FormaDTO();

        dto.setId(zd.getId());
        dto.setNomerZayavleniya(zd.getNum());
        dto.setIstochnik(zd.getIdSour());
        dto.setIdSourType(zd.getIdSourType());
        dto.setVidZayavleniya(zd.getIdTip());
        dto.setOsnova(zd.getIdOsn());
        dto.setDateObr(zd.getDInp());
        dto.setDatePrivem(zd.getDInpDoc());
        dto.setYazykZayavl(zd.getDoclang());
        dto.setDomTel(zd.getHomePhone());
        dto.setMobTel(zd.getMobilePhone());
        dto.setPribyl(zd.getIdExtCntr() != null || zd.getIdExtBr() != null);
        dto.setStranaPrib(zd.getIdExtCntr() != null ? zd.getIdExtCntr().toString() : null);
        dto.setBrid(zd.getBrid());
        dto.setInpBrid(zd.getInpBrid());
        dto.setDReg(zd.getDReg());
        dto.setEstDate(zd.getEstDate());
        dto.setIsOtkaz(zd.getIsOtkaz());
        dto.setConNum(zd.getConNum());
        dto.setConDat(zd.getConDat() != null ? zd.getConDat().toLocalDate() : null);
        dto.setSolId(zd.getSicid() != null ? zd.getSicid().toString() : null);

        if (eg != null) {
            dto.setIin(eg.getIin());
            String fio = (eg.getLn() != null ? eg.getLn() : "") + " "
                    + (eg.getFn() != null ? eg.getFn() : "")
                    + (eg.getMn() != null ? " " + eg.getMn() : "");
            dto.setFio(fio.trim());
            dto.setDateBirth(eg.getBd());
        }

        // Ищем m_sol: сначала по номеру заявления, потом по общему ID
        Optional<com.example.demo.entity.MSol> solOpt = solRepo.findByZNumb(zd.getNum());
        if (!solOpt.isPresent()) {
            solOpt = solRepo.findById(zd.getId());
        }
        solOpt.ifPresent(sol -> {
            dto.setNResh(sol.getNResh());
            dto.setDResh(sol.getDResh());
            dto.setNomerDela(sol.getNumb());
            dto.setMaketId(sol.getMid());
            dto.setMaketNaznSumma(sol.getNsum());

            payRepo.findBySid(sol.getId()).ifPresent(pay -> {
                dto.setPayId(pay.getId());
                dto.setSposobViplaty(pay.getPc());
                dto.setMaketDateNazn(pay.getDNaz());
                dto.setMaketDateOkon(pay.getStopdate());
            });

            solStRepo.findTopBySidOrderByDatDesc(sol.getId()).ifPresent(st -> {
                dto.setStatus(st.getSt2());
                dto.setLastStatusDate(st.getDat());
                dto.setLastStatusUser(st.getUsr());
                dto.setRejectReason(st.getRetTxt());
            });
        });

        return dto;
    }
}