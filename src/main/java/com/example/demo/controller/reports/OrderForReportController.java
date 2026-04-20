package com.example.demo.controller.reports;

import com.example.demo.entity.reports.OrderForReport;
import com.example.demo.repository.reports.OrderForReportRepository;
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
    private final JdbcTemplate jdbcTemplate;
    private final XrepService xrepService;

    public OrderForReportController(OrderForReportRepository repository,
                                    JdbcTemplate jdbcTemplate,
                                    XrepService xrepService) {
        this.repository   = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.xrepService  = xrepService;
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

        try {
            String sql = String.format(
                    "SELECT em5.run_rep(%d, '%s', '%s')",
                    req.idReport(),
                    req.params() != null ? req.params().replace("'", "''") : "",
                    "admin"
            );
            String jsonResult = jdbcTemplate.queryForObject(sql, String.class);
            if (jsonResult != null) {
                if (jsonResult.contains("\"result\": 1") || jsonResult.contains("\"result\":1")) {
                    order.setStatus((short) 1);
                    order.setReport(jsonResult);
                } else {
                    order.setStatus((short) 2);
                    order.setErr(jsonResult);
                }
            }
        } catch (Exception e) {
            try {
                String htmlReport = generateReportByXrep(req);
                order.setStatus((short) 1);
                order.setReport("{\"result\":1,\"html\":\"" + escapeJson(htmlReport) + "\"}");
            } catch (Exception ex) {
                order.setStatus((short) 2);
                order.setErr("XrepService error: " + ex.getMessage());
            }
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
        result.put("id", order.getId());
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

    private String generateReportByXrep(OrderRequest req) {
        String sql = getReportSql(req.idReport(), req);
        Map<Integer, String> headers = getReportHeaders(req.idReport());
        String title = getReportTitle(req.idReport());
        return xrepService.genTableSelect(sql, headers, title, null, true, 1);
    }

    private String getReportSql(Long repId, OrderRequest req) {
        // TODO: добавь SQL для каждого отчёта по repId
        return "SELECT 'Отчёт #" + repId + "' AS report, 0 AS count";
    }

    private Map<Integer, String> getReportHeaders(Long repId) {
        Map<Integer, String> h = new LinkedHashMap<>();
        h.put(1, "Наименование");
        h.put(2, "Количество");
        return h;
    }

    private String getReportTitle(Long repId) {
        return "Отчёт № " + repId;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public record OrderRequest(Long idReport, String params, Long empId, String begDate, String endDate) {}
}