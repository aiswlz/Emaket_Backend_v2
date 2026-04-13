package com.example.demo.service;

import com.example.demo.dto.ElZayavkaDTO;
import com.example.demo.repository.MEgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElZayavkaService {

    @Autowired private MEgRepository egRepo;

    public List<ElZayavkaDTO> getAll() {
        return egRepo.findAll().stream().map(eg -> {
            ElZayavkaDTO dto = new ElZayavkaDTO();
            dto.setId(eg.getId());
            dto.setData(eg.getDat());
            dto.setNomerZayavleniya(eg.getZnum());
            dto.setOtdelenie(eg.getBrid());
            dto.setIin(eg.getIin());
            dto.setFio(eg.getLn() + " " + eg.getFn() + " " +
                    (eg.getMn() != null ? eg.getMn() : ""));
            dto.setDataRozhdeniya(eg.getBd());
            dto.setStatus(eg.getIdSt());
            dto.setOsnova(eg.getIdOsn());
            dto.setIstochnik(eg.getIdSour());
            dto.setTipistochnika(eg.getIdSourType());
            dto.setKommentariy(eg.getComm());
            return dto;
        }).collect(Collectors.toList());
    }
}