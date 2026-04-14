package com.example.demo.controller;

import com.example.demo.dto.MacetDTO;
import com.example.demo.service.MacetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maket")
@CrossOrigin(origins = "http://localhost:4200")
public class MacetController {

    @Autowired private MacetService service;

    @PostMapping
    public ResponseEntity<MacetDTO> save(@RequestBody MacetDTO dto) {
        return ResponseEntity.ok(service.save(dto));
    }
}