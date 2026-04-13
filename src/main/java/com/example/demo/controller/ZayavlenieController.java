package com.example.demo.controller;

import com.example.demo.dto.ZayavlenieDTO;
import com.example.demo.service.ZayavlenieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/zayavleniya")
@CrossOrigin(origins = "http://localhost:4200")
public class ZayavlenieController {

    @Autowired private ZayavlenieService service;

    @GetMapping
    public List<ZayavlenieDTO> getAll() {
        return service.getAll();
    }
}