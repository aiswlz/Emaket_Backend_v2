package com.example.demo.controller.reports;

import com.example.demo.entity.reports.ReportGfssView;
import com.example.demo.repository.reports.ReportGfssRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")
public class ReportGfssController {

    private final ReportGfssRepository repository;

    public ReportGfssController(ReportGfssRepository repository) {
        this.repository = repository;
    }

    // GET /api/reports/gfss — список отчётов (без дублей и без HTML)
    @GetMapping("/gfss")
    public List<ReportGfssDto> getGfssReports() {
        return repository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        ReportGfssView::getId,   // ключ — id
                        r -> r,                   // значение
                        (a, b) -> a               // если дубль — берём первый
                ))
                .values()
                .stream()
                .map(r -> new ReportGfssDto(
                        r.getId(),
                        r.getParentId(),
                        r.getActionType(),
                        r.getNameRus(),
                        r.getNameKaz(),
                        r.getRepId(),
                        r.getCmd(),
                        r.getAsync(),
                        r.getOrd(),
                        r.getLev1(),      // ← добавить
                        r.getLev1Kz()
                ))
                .sorted((a, b) -> Integer.compare(
                        a.ord() != null ? a.ord() : 0,
                        b.ord() != null ? b.ord() : 0
                ))
                .collect(Collectors.toList());
    }

    // DTO — только нужные поля, без lev1 HTML
    public record ReportGfssDto(
            Long id,
            Long parentId,
            Integer actionType,
            String nameRus,
            String nameKaz,
            Long repId,
            String cmd,
            String async,
            Integer ord,
            String lev1,
            String lev1Kz
    ) {}
}