package com.example.demo.controller;

import com.example.demo.entity.ZHistory;
import com.example.demo.repository.ZHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "http://localhost:4200")
public class ZHistoryController {

    @Autowired
    private ZHistoryRepository historyRepo;

    // Получить историю по zdoc_id
    @GetMapping("/by-zdoc/{zdocId}")
    public List<ZHistory> getByZdocId(@PathVariable Long zdocId) {
        return historyRepo.findByZdocIdOrderByDatDesc(zdocId);
    }

    // Сохранить запись в историю
    @PostMapping
    public ZHistory save(@RequestBody ZHistory history) {
        history.setDat(LocalDateTime.now());
        return historyRepo.save(history);
    }
}