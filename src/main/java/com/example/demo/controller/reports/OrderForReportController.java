package com.example.demo.controller.reports;

import com.example.demo.entity.reports.OrderForReport;
import com.example.demo.repository.reports.OrderForReportRepository;
import com.example.demo.service.reports.ReportSqlRegistry;
import com.example.demo.service.reports.XrepService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderForReportController {

    private final OrderForReportRepository repository;
    private final JdbcTemplate             jdbcTemplate;
    private final XrepService              xrepService;
    private final ReportSqlRegistry        sqlRegistry;

    public OrderForReportController(OrderForReportRepository repository,
                                    JdbcTemplate jdbcTemplate,
                                    XrepService xrepService,
                                    ReportSqlRegistry sqlRegistry) {
        this.repository   = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.xrepService  = xrepService;
        this.sqlRegistry  = sqlRegistry;
    }

    @PostMapping("/order")
    public OrderForReport createOrder(@RequestBody OrderRequest req) {

        OrderForReport order = new OrderForReport();
        order.setIdReport(req.idReport());
        order.setParams(req.params());
        order.setEmpId(req.empId() != null ? req.empId() : 1L);
        order.setStatus((short) 0);
        order.setDat(LocalDateTime.now());
        order = repository.save(order);

        // Шаг 1: Пробуем em5.run_rep
        try {
            String sql = String.format(
                    "SELECT em5.run_rep(%d, '%s', '%s')",
                    req.idReport(),
                    req.params() != null ? req.params().replace("'", "''") : "",
                    "admin"
            );
            String jsonResult = jdbcTemplate.queryForObject(sql, String.class);

            boolean isRealResult = jsonResult != null
                    && (jsonResult.contains("\"result\": 1") || jsonResult.contains("\"result\":1"))
                    && !jsonResult.contains("\"cmd\"");

            if (isRealResult) {
                order.setStatus((short) 1);
                order.setReport(jsonResult);
                return repository.save(order);
            }
        } catch (Exception ignored) {}

        // Шаг 2: ReportSqlRegistry
        String begDate = coalesce(extractParam(req.params(), "bdat"), req.begDate());
        String endDate = coalesce(extractParam(req.params(), "edat"), req.endDate());
        String payment = extractParam(req.params(), "payment");
        String cntDay  = extractParam(req.params(), "cnt_day");

        ReportSqlRegistry.ReportDefinition def =
                sqlRegistry.get(req.idReport(), begDate, endDate, payment, cntDay);

        if (def != null) {
            try {
                LocalDateTime start = LocalDateTime.now();

                // Проверяем есть ли данные
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(def.sql);

                if (rows.isEmpty()) {
                    // Данных нет → статус 2
                    order.setStatus((short) 2);
                    order.setErr("Данные за выбранный период отсутствуют");
                } else {
                    // Данные есть → строим HTML
                    String htmlReport = xrepService.genTableSelect(
                            def.sql,
                            def.headers,
                            def.title,
                            null,
                            true,
                            1
                    );
                    htmlReport += xrepService.endTimeReport(start, LocalDateTime.now());

                    order.setStatus((short) 1);
                    order.setReport("{\"result\":1,\"html\":\""
                            + escapeJson(htmlReport) + "\"}");
                }

            } catch (Exception ex) {
                order.setStatus((short) 2);
                order.setErr("Ошибка выполнения SQL для отчёта #"
                        + req.idReport() + ": " + ex.getMessage());
            }
        } else {
            order.setStatus((short) 2);
            order.setErr("Отчёт #" + req.idReport()
                    + " ещё не реализован. Добавьте SQL в ReportSqlRegistry.java");
        }

        return repository.save(order);
    }

    @GetMapping("/order/{id}")
    public OrderForReport getOrder(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    @GetMapping("/order/{id}/html")
    public Map<String, Object> getOrderHtml(@PathVariable Long id) {
        OrderForReport order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",     order.getId());
        result.put("status", order.getStatus());

        if (order.getStatus() == 1 && order.getReport() != null) {
            String report = order.getReport();
            if (report.contains("\"html\":")) {
                int start = report.indexOf("\"html\":\"") + 8;
                int end   = report.lastIndexOf("\"");
                if (end > start) result.put("html", unescapeJson(report.substring(start, end)));
                else result.put("html", report);
            } else {
                result.put("html", report);
            }
        } else if (order.getErr() != null) {
            result.put("error", order.getErr());
        }
        return result;
    }

    private String extractParam(String params, String name) {
        if (params == null || params.isBlank()) return null;
        for (String pair : params.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(name)) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n",  "\n")
                .replace("\\r",  "\r")
                .replace("\\t",  "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public record OrderRequest(
            Long   idReport,
            String params,
            Long   empId,
            String begDate,
            String endDate
    ) {}
}