package com.example.demo.controller.reports;

import com.example.demo.service.reports.XrepService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Контроллер для генерации отчётов через XrepService.
 * Аналог вызовов процедур xrep из Oracle.
 */
@RestController
@RequestMapping("/api/xrep")
@CrossOrigin(origins = "http://localhost:4200")
public class XrepReportController {

    private final XrepService xrepService;

    public XrepReportController(XrepService xrepService) {
        this.xrepService = xrepService;
    }

    // ─── POST /api/xrep/table-select ─────────────────────────────────────────
    /**
     * Аналог gen_table_select.
     * Принимает SQL-запрос и параметры, возвращает HTML-таблицу.
     *
     * Пример запроса:
     * {
     *   "sql": "SELECT id, name, cnt FROM em5.some_view WHERE status = 1",
     *   "repBeg": "Список записей",
     *   "calcTotal": true,
     *   "drPp": 1,
     *   "headers": { "1": "ID", "2": "Наименование", "3": "Кол-во" }
     * }
     */
    @PostMapping("/table-select")
    public ReportHtmlResponse tableSelect(@RequestBody TableSelectRequest req) {
        LocalDateTime start = LocalDateTime.now();

        Map<Integer, String> headers = new LinkedHashMap<>();
        if (req.headers() != null) {
            req.headers().forEach((k, v) -> headers.put(Integer.parseInt(k), v));
        }

        String html = xrepService.genTableSelect(
                req.sql(),
                headers,
                req.repBeg(),
                req.cntCol(),
                Boolean.TRUE.equals(req.calcTotal()),
                req.drPp() != null ? req.drPp() : 1
        );

        html += xrepService.endTimeReport(start, LocalDateTime.now());
        return new ReportHtmlResponse(html);
    }

    // ─── POST /api/xrep/gen-table ─────────────────────────────────────────────
    /**
     * Аналог gen_table.
     * Принимает данные в формате Tab_Array и строит кросс-таблицу.
     *
     * Пример запроса:
     * {
     *   "rows": [
     *     { "rowKey": "region|001", "valStr": "001", "valName": "Алматы", "colName": "Январь", "cntAll": 15 }
     *   ],
     *   "tFirstColumnName": "Регион",
     *   "repBeg": "Отчёт по регионам",
     *   "drPp": 1,
     *   "isProcent": 0
     * }
     */
    @PostMapping("/gen-table")
    public ReportHtmlResponse genTable(@RequestBody GenTableRequest req) {
        LocalDateTime start = LocalDateTime.now();

        Map<String, XrepService.TabRow> tabWork = new LinkedHashMap<>();

        if (req.rows() != null) {
            for (TabRowInput r : req.rows()) {
                XrepService.TabRow row = new XrepService.TabRow();
                row.valStr   = r.valStr();
                row.valName  = r.valName();
                row.cntAll   = r.cntAll() != null ? r.cntAll() : 0;
                row.cntAll1  = r.cntAll1() != null ? r.cntAll1() : 0;
                row.colName  = r.colName();
                row.colName1 = r.colName1();
                tabWork.put(r.rowKey(), row);
            }
        }

        String html = xrepService.genTable(
                tabWork,
                req.tFirstColumn()     != null ? req.tFirstColumn()     : "key",
                req.tFirstColumnName() != null ? req.tFirstColumnName() : "Наименование",
                req.tColumn()          != null ? req.tColumn()          : "col",
                req.tColumnName()      != null ? req.tColumnName()      : "Колонка",
                req.repBeg(),
                req.isNotNull()  != null ? req.isNotNull()  : 0,
                req.valNameNull() != null ? req.valNameNull() : "",
                req.drPp()       != null ? req.drPp()       : 1,
                req.isProcent()  != null ? req.isProcent()  : 0
        );

        html += xrepService.endTimeReport(start, LocalDateTime.now());
        return new ReportHtmlResponse(html);
    }

    // ─── GET /api/xrep/hl-beg, hl-end — утилиты ─────────────────────────────

    @GetMapping("/hl-beg")
    public Map<String, String> hlBeg(
            @RequestParam(defaultValue = "blue") String color,
            @RequestParam(defaultValue = "true") boolean underline,
            @RequestParam(defaultValue = "true") boolean bold) {
        return Map.of("html", xrepService.hlBeg(color, underline, bold));
    }

    @GetMapping("/hl-end")
    public Map<String, String> hlEnd(
            @RequestParam(defaultValue = "true") boolean underline,
            @RequestParam(defaultValue = "true") boolean bold) {
        return Map.of("html", xrepService.hlEnd(underline, bold));
    }

    // ─── DTO / Records ────────────────────────────────────────────────────────

    public record ReportHtmlResponse(String html) {}

    public record TableSelectRequest(
            String              sql,
            String              repBeg,
            Integer             cntCol,
            Boolean             calcTotal,
            Integer             drPp,
            Map<String, String> headers
    ) {}

    public record GenTableRequest(
            List<TabRowInput> rows,
            String tFirstColumn,
            String tFirstColumnName,
            String tColumn,
            String tColumnName,
            String repBeg,
            Integer isNotNull,
            String  valNameNull,
            Integer drPp,
            Integer isProcent
    ) {}

    public record TabRowInput(
            String rowKey,
            String valStr,
            String valName,
            Integer cntAll,
            Integer cntAll1,
            String colName,
            String colName1
    ) {}
}