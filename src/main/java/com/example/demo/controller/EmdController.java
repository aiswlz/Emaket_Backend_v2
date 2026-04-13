package com.example.demo.controller;

import com.example.demo.dto.EmdDTO;
import com.example.demo.service.EmdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/emd")
@CrossOrigin(origins = "http://localhost:4200")
public class EmdController {

    @Autowired private EmdService service;

    @GetMapping
    public List<EmdDTO> getAll() {
        return service.getAll();
    }
}