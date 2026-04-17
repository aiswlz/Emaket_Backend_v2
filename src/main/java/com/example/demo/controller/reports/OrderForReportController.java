package com.example.demo.controller.reports;

import com.example.demo.entity.reports.OrderForReport;
import com.example.demo.repository.reports.OrderForReportRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderForReportController {

    private final OrderForReportRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public OrderForReportController(OrderForReportRepository repository,
                                    JdbcTemplate jdbcTemplate) {
        this.repository   = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // POST /api/reports/order
    @PostMapping("/order")
    public OrderForReport createOrder(@RequestBody OrderRequest req) {

        // Создаём заказ со статусом 0 (в очереди)
        OrderForReport order = new OrderForReport();
        order.setIdReport(req.idReport());
        order.setParams(req.params());
        order.setEmpId(req.empId() != null ? req.empId() : 1L);
        order.setStatus((short) 0);
        order.setDat(LocalDateTime.now());
        order = repository.save(order);

        try {
            // Вызываем функцию которая возвращает JSONB
            String sql = String.format(
                    "SELECT em5.run_rep(%d, '%s', '%s')",
                    req.idReport(),
                    req.params() != null ? req.params().replace("'", "''") : "",
                    "admin"
            );

            String jsonResult = jdbcTemplate.queryForObject(sql, String.class);

            if (jsonResult != null) {
                if (jsonResult.contains("\"result\": 1") || jsonResult.contains("\"result\":1")) {
                    order.setStatus((short) 1);  // готов
                    order.setReport(jsonResult); // ← сохраняем в report
                } else {
                    order.setStatus((short) 2);  // ошибка
                    order.setErr(jsonResult);
                }
            }
        } catch (Exception e) {
            order.setStatus((short) 2);
            order.setErr(e.getMessage());
        }

        return repository.save(order);
    }

    // GET /api/reports/order/{id}
    @GetMapping("/order/{id}")
    public OrderForReport getOrder(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public record OrderRequest(
            Long   idReport,
            String params,
            Long   empId,
            String begDate,
            String endDate
    ) {}
}