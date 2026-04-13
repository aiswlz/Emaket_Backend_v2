package com.example.demo.service;

import com.example.demo.dto.FormaDTO;
import com.example.demo.entity.MEg;
import com.example.demo.entity.ZDoc;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        return egRepo.findByIin(iin).flatMap(eg -> {
            if (eg.getZnum() == null) return Optional.empty();
            return zDocRepo.findByNum(eg.getZnum())
                    .map(zd -> buildDto(eg, zd));
        });
    }

    // ── НОВЫЙ МЕТОД ──────────────────────────────────────────────
    public Optional<FormaDTO> update(Long id, FormaDTO dto) {
        return zDocRepo.findById(id).map(zd -> {

            if (dto.getDateObr() != null)        zd.setDInp(dto.getDateObr());
            if (dto.getDatePrivem() != null)      zd.setDInpDoc(dto.getDatePrivem());
            if (dto.getYazykZayavl() != null)     zd.setDoclang(dto.getYazykZayavl());
            if (dto.getDomTel() != null)          zd.setHomePhone(dto.getDomTel());
            if (dto.getMobTel() != null)          zd.setMobilePhone(dto.getMobTel());
            if (dto.getIstochnik() != null)       zd.setIdSour(dto.getIstochnik());
            if (dto.getVidZayavleniya() != null)  zd.setIdTip(dto.getVidZayavleniya());

            zDocRepo.save(zd);

            MEg eg = null;
            if (dto.getIin() != null) {
                Optional<MEg> egOpt = egRepo.findByIin(dto.getIin());
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
    // ─────────────────────────────────────────────────────────────

    private FormaDTO buildDto(MEg eg, ZDoc zd) {
        FormaDTO dto = new FormaDTO();

        dto.setId(zd.getId());
        dto.setNomerZayavleniya(zd.getNum());
        dto.setIstochnik(zd.getIdSour());
        dto.setIdSourType(zd.getIdSourType());
        dto.setVidZayavleniya(zd.getIdTip());
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
        dto.setConDat(zd.getConDat());
        dto.setSolId(zd.getSicid() != null ? zd.getSicid().toString() : null);

        if (eg != null) {
            dto.setIin(eg.getIin());
            String fio = (eg.getLn() != null ? eg.getLn() : "") + " "
                    + (eg.getFn() != null ? eg.getFn() : "")
                    + (eg.getMn() != null ? " " + eg.getMn() : "");
            dto.setFio(fio.trim());
            dto.setDateBirth(eg.getBd());
        }

        solRepo.findByZNumb(zd.getNum()).ifPresent(sol -> {
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