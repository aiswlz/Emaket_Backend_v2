package com.example.demo.service;

import com.example.demo.dto.FormaDTO;
import com.example.demo.entity.MEg;
import com.example.demo.entity.ZDoc;
import com.example.demo.entity.ZHistory;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired private ZHistoryRepository historyRepo;

    // ─── GET BY ID ────────────────────────────────────────────────
    public Optional<FormaDTO> getById(Long id) {
        return zDocRepo.findById(id).map(zd -> {
            MEg eg = null;
            if (zd.getIdEg() != null) {
                eg = egRepo.findById(zd.getIdEg()).orElse(null);
            }
            if (eg == null) {
                eg = egRepo.findById(id).orElse(null);
            }
            return buildDto(eg, zd);
        });
    }

    // ─── GET BY IIN (один z_doc) ──────────────────────────────────
    public Optional<FormaDTO> getByIin(Long iin) {
        return egRepo.findFirstByIin(iin).flatMap(eg -> {
            Optional<ZDoc> zdOpt = zDocRepo.findFirstByIdEg(eg.getId());
            if (zdOpt.isEmpty()) {
                zdOpt = zDocRepo.findById(eg.getId());
            }
            return zdOpt.map(zd -> buildDto(eg, zd));
        });
    }

    // ─── GET ALL BY IIN (все z_doc клиента) ───────────────────────
    public List<FormaDTO> getAllByIin(Long iin) {
        Optional<MEg> egOpt = egRepo.findFirstByIin(iin);
        if (egOpt.isEmpty()) return new ArrayList<>();

        MEg eg = egOpt.get();
        List<FormaDTO> result = new ArrayList<>();

        List<ZDoc> zdocList = zDocRepo.findAllByIdEg(eg.getId());
        if (zdocList.isEmpty()) {
            zDocRepo.findById(eg.getId()).ifPresent(zdocList::add);
        }

        for (ZDoc zd : zdocList) {
            result.add(buildDto(eg, zd));
        }
        return result;
    }

    // ─── GET CLIENT BY IIN ────────────────────────────────────────
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

    // ─── CREATE ───────────────────────────────────────────────────
    public FormaDTO create(FormaDTO dto) {

        LocalDate today = LocalDate.now();

        // Только поиск — никакого создания MEg
        MEg eg = dto.getIin() != null
                ? egRepo.findFirstByIin(dto.getIin()).orElse(null)
                : null;

        if (eg == null) {
            throw new RuntimeException(
                    "Клиент с ИИН " + dto.getIin() + " не найден в базе данных"
            );
        }

        String nomerZayavl = dto.getNomerZayavleniya() != null
                ? dto.getNomerZayavleniya()
                : String.valueOf(System.currentTimeMillis());

        // Обрезаем brid до 3 символов (z_doc.brid = varchar(3))
        String rawBrid = eg.getBrid() != null ? eg.getBrid().trim() : "000";
        String brid = rawBrid.length() > 3 ? rawBrid.substring(0, 3) : rawBrid;

        Long sicid = eg.getIdAcc() != null ? eg.getIdAcc() : eg.getId();

        ZDoc zd = new ZDoc();
        zd.setIdEg(eg.getId());
        zd.setNum(nomerZayavl);
        zd.setConNum(nomerZayavl);
        zd.setConDat(LocalDateTime.now());
        zd.setDInp(dto.getDateObr() != null ? dto.getDateObr() : today);
        zd.setDInpDoc(dto.getDatePrivem() != null ? dto.getDatePrivem() : today);
        zd.setDReg(today);
        zd.setDat(LocalDateTime.now());
        zd.setIdTip("NEW");
        zd.setDoclang(mapLang(dto.getYazykZayavl()));
        zd.setIsOtkaz(0L);
        zd.setEstDate(today.plusMonths(2));
        zd.setEstChange(1L);
        zd.setBrid(brid);
        zd.setSicid(sicid);

        Long idOsnToUse = dto.getIdOsn() != null ? dto.getIdOsn()
                : dto.getOsnova() != null ? dto.getOsnova()
                  : eg.getIdOsn() != null ? eg.getIdOsn() : 103L;
        zd.setIdOsn(idOsnToUse);

        String idSour = eg.getIdSour() != null ? eg.getIdSour() : "WEB";
        zd.setIdSour(idSour);

        String idSourType = eg.getIdSourType() != null ? eg.getIdSourType() : "PRA";
        zd.setIdSourType(idSourType);

        zd.setMobilePhone(dto.getMobTel() != null ? dto.getMobTel() : eg.getMobilePhone());
        zd.setMobileSource("WEB");
        zd.setHomePhone(dto.getDomTel());
        zd.setIdEmp(1L);
        zd.setUsr("EMA");

        zDocRepo.save(zd);

        eg.setZnum(nomerZayavl);
        egRepo.save(eg);

        return buildDto(eg, zd);
    }

    // ─── DELETE ───────────────────────────────────────────────────
    @Transactional
    public void deleteById(Long id) {
        // 1. Удаляем историю по zdoc_id
        List<ZHistory> history = historyRepo.findByZdocIdOrderByDatDesc(id);
        if (!history.isEmpty()) {
            historyRepo.deleteAll(history);
        }

        // 2. Удаляем m_sol_st, m_pay, m_sol если есть
        solRepo.findById(id).ifPresent(sol -> {
            solStRepo.findTopBySidOrderByDatDesc(sol.getId())
                    .ifPresent(st -> solStRepo.delete(st));
            payRepo.findBySid(sol.getId())
                    .ifPresent(pay -> payRepo.delete(pay));
            solRepo.delete(sol);
        });

        // 3. Удаляем z_doc
        if (zDocRepo.existsById(id)) {
            zDocRepo.deleteById(id);
        }
    }

    // ─── UPDATE ───────────────────────────────────────────────────
    public Optional<FormaDTO> update(Long id, FormaDTO dto) {
        return zDocRepo.findById(id).map(zd -> {

            if (dto.getDateObr() != null)     zd.setDInp(dto.getDateObr());
            if (dto.getDatePrivem() != null)  zd.setDInpDoc(dto.getDatePrivem());
            if (dto.getYazykZayavl() != null) zd.setDoclang(mapLang(dto.getYazykZayavl()));
            if (dto.getDomTel() != null)      zd.setHomePhone(dto.getDomTel());
            if (dto.getMobTel() != null)      zd.setMobilePhone(dto.getMobTel());

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

    // ─── HELPERS ──────────────────────────────────────────────────

    // Маппинг языка: "Русский" → "ru", "Казахский" → "kz"
    private String mapLang(String lang) {
        if (lang == null) return "ru";
        switch (lang.trim().toLowerCase()) {
            case "казахский":
            case "қазақша":
            case "kaz":
            case "kz":
                return "kz";
            default:
                return "ru";
        }
    }

    private FormaDTO buildDto(MEg eg, ZDoc zd) {
        FormaDTO dto = new FormaDTO();

        dto.setId(zd.getId());
        dto.setNomerZayavleniya(zd.getNum());
        dto.setIstochnik(zd.getIdSour());
        dto.setIdSourType(zd.getIdSourType());
        dto.setVidZayavleniya(zd.getIdTip());
        dto.setOsnova(zd.getIdOsn());
        dto.setIdOsn(zd.getIdOsn());
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