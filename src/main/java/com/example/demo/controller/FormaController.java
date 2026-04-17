package com.example.demo.controller;

import com.example.demo.dto.FormaDTO;
import com.example.demo.service.FormaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forma")
@CrossOrigin(origins = "http://localhost:4200")
public class FormaController {

    @Autowired private FormaService service;

    @PostMapping
    public ResponseEntity<FormaDTO> create(@RequestBody FormaDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }
    @GetMapping("/{id}")
    public ResponseEntity<FormaDTO> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-iin/{iin}")
    public ResponseEntity<FormaDTO> getByIin(@PathVariable Long iin) {
        return service.getByIin(iin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<FormaDTO> update(@PathVariable Long id, @RequestBody FormaDTO dto) {
        return service.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/all-by-iin/{iin}")
    public ResponseEntity<List<FormaDTO>> getAllByIin(@PathVariable Long iin) {
        return ResponseEntity.ok(service.getAllByIin(iin));
    }
    @GetMapping("/client/{iin}")
    public ResponseEntity<FormaDTO> getClientByIin(@PathVariable Long iin) {
        return service.getClientByIin(iin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}