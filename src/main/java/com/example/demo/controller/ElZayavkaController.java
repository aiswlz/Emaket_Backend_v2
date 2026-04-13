package com.example.demo.controller;

import com.example.demo.dto.ElZayavkaDTO;
import com.example.demo.service.ElZayavkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/zayavki")
@CrossOrigin(origins = "http://localhost:4200")
public class ElZayavkaController {

    @Autowired private ElZayavkaService service;

    @GetMapping
    public List<ElZayavkaDTO> getAll() {
        return service.getAll();
    }
}