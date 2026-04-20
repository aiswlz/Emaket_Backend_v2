package com.example.demo.service.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Аналог Oracle-пакета xrep.
 * Генерирует HTML-таблицы для отчётов.
 */
@Service
public class XrepService {

    private final JdbcTemplate jdbcTemplate;

    // ─── Внутренние типы (аналог TYPE...IS RECORD в PL/SQL) ──────────────────

    public static class TabRow {
        public int    id;
        public int    valInt;
        public String valStr;
        public String valName;
        public int    cntAll;
        public int    cntAll1;
        public String colName;
        public String colName1;
    }

    // Tab_Array  ↔  Map<String, TabRow>
    // Tab_Array_List ↔  Map<String, String[]>  (x1..x30)

    public static final int CNT_COLUMNS = 30;

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /** Аналог xHL_Beg — открывающий тег ссылки/жирного/подчёркнутого */
    public String hlBeg(String color, boolean underline, boolean bold) {
        StringBuilder sb = new StringBuilder();
        if (bold)      sb.append("<b>");
        if (underline) sb.append("<u>");
        sb.append("<span style=\"color:").append(color).append("\">");
        return sb.toString();
    }

    public String hlBeg() { return hlBeg("blue", true, true); }

    /** Аналог xHL_End */
    public String hlEnd(boolean underline, boolean bold) {
        StringBuilder sb = new StringBuilder();
        sb.append("</span>");
        if (underline) sb.append("</u>");
        if (bold)      sb.append("</b>");
        return sb.toString();
    }

    public String hlEnd() { return hlEnd(true, true); }

    /** Аналог rr(x_cnt) — перевод строки × N */
    public String rr(int cnt) {
        return "<br>".repeat(Math.max(cnt, 0));
    }

    /** Аналог repl — экранирование спецсимволов для HTML */
    public String repl(String sv) {
        if (sv == null) return "";
        return sv.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Аналог get_high_text — выделение текста цветом по номеру */
    public String getHighText(int tNum, String txt) {
        String[] colors = {"", "red", "green", "blue", "orange", "purple"};
        String color = (tNum > 0 && tNum < colors.length) ? colors[tNum] : "black";
        return "<span style=\"color:" + color + "\">" + txt + "</span>";
    }

    /** Аналог get_tab_value — получить значение из Tab_Array_List по номеру колонки */
    public String getTabValue(String idx, int num, Map<String, String[]> tabWork) {
        String[] row = tabWork.get(idx);
        if (row == null || num < 1 || num > row.length) return "";
        return row[num - 1] != null ? row[num - 1] : "";
    }

    // ─── ins_tab — добавить запись в Tab_Array ────────────────────────────────

    /**
     * Аналог ins_tab.
     * Заполняет Map<String, TabRow> по ключу xFNameTab+xGroupVal.
     */
    public void insTab(Map<String, TabRow> tabWork,
                       String fNameTab,
                       String groupVal,
                       String valStr,
                       String valName,
                       int    valCnt,
                       int    valCnt1,
                       String colName,
                       String colName1) {

        String key = fNameTab + "|" + groupVal;
        TabRow row = tabWork.computeIfAbsent(key, k -> new TabRow());
        row.valStr   = valStr;
        row.valName  = valName;
        row.cntAll  += valCnt;
        row.cntAll1 += valCnt1;
        row.colName  = colName;
        row.colName1 = colName1;
    }

    // ─── gen_table ────────────────────────────────────────────────────────────

    /**
     * Аналог gen_table.
     * Строит HTML-таблицу с динамическими колонками из Tab_Array.
     *
     * @param tabWork        заполненный Map (аналог Tab_Work)
     * @param tFirstColumn   поле-идентификатор строк (группировка по вертикали)
     * @param tFirstColumnName поле-наименование строк
     * @param tColumn        поле-идентификатор колонок (группировка по горизонтали)
     * @param tColumnName    поле-наименование колонок
     * @param repBeg         текст над таблицей
     * @param isNotNull      пропускать "пустые" записи (1=да)
     * @param valNameNull    строка обозначающая "пустое" значение
     * @param drPp           рисовать колонку № п/п (1=да)
     * @param isProcent      добавить колонку % (1=да)
     * @return HTML-строка таблицы
     */
    public String genTable(Map<String, TabRow> tabWork,
                           String tFirstColumn,
                           String tFirstColumnName,
                           String tColumn,
                           String tColumnName,
                           String repBeg,
                           int    isNotNull,
                           String valNameNull,
                           int    drPp,
                           int    isProcent) {

        // 1. Собираем уникальные строки и колонки
        LinkedHashSet<String> rowKeys = new LinkedHashSet<>();
        LinkedHashSet<String> colKeys = new LinkedHashSet<>();

        for (Map.Entry<String, TabRow> e : tabWork.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            if (parts.length == 2) {
                String rk = parts[0];
                String ck = parts[1];
                if (isNotNull == 1 && valNameNull != null && ck.contains(valNameNull)) continue;
                rowKeys.add(rk);
                colKeys.add(ck);
            }
        }

        List<String> rows = new ArrayList<>(rowKeys);
        List<String> cols = new ArrayList<>(colKeys);
        Collections.sort(cols); // сортировка колонок текстовая (как в PL/SQL)

        // 2. Строим HTML
        StringBuilder html = new StringBuilder();

        if (repBeg != null && !repBeg.isEmpty()) {
            html.append("<p><b>").append(repBeg).append("</b></p>\n");
        }

        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\">\n");

        // Заголовок
        html.append("<thead><tr style=\"background:#d0e4f7\">");
        if (drPp == 1) html.append("<th>№</th>");
        html.append("<th>").append(repl(tFirstColumnName)).append("</th>");

        // Определяем имя для каждой колонки из данных
        Map<String, String> colNames = new LinkedHashMap<>();
        for (String ck : cols) {
            for (Map.Entry<String, TabRow> e : tabWork.entrySet()) {
                if (e.getKey().endsWith("|" + ck)) {
                    colNames.put(ck, e.getValue().colName != null ? e.getValue().colName : ck);
                    break;
                }
            }
            html.append("<th>").append(repl(colNames.getOrDefault(ck, ck))).append("</th>");
        }
        html.append("<th>Итого</th>");
        if (isProcent == 1) html.append("<th>%</th>");
        html.append("</tr></thead>\n<tbody>\n");

        // Итог по колонкам для строки "Итого"
        Map<String, Integer> colTotals = new LinkedHashMap<>();
        int grandTotal = 0;
        int ppNum = 0;

        for (String rk : rows) {
            int rowTotal = 0;
            StringBuilder rowHtml = new StringBuilder();
            rowHtml.append("<tr>");
            if (drPp == 1) rowHtml.append("<td>").append(++ppNum).append("</td>");

            // Найдём имя строки
            String rowName = rk;
            for (Map.Entry<String, TabRow> e : tabWork.entrySet()) {
                if (e.getKey().startsWith(rk + "|")) {
                    TabRow r = e.getValue();
                    rowName = r.valName != null ? r.valName : rk;
                    break;
                }
            }
            rowHtml.append("<td>").append(repl(rowName)).append("</td>");

            for (String ck : cols) {
                String key = rk + "|" + ck;
                TabRow cell = tabWork.get(key);
                int val = (cell != null) ? cell.cntAll : 0;
                rowTotal += val;
                colTotals.merge(ck, val, Integer::sum);
                rowHtml.append("<td style=\"text-align:center\">")
                        .append(val > 0 ? val : "")
                        .append("</td>");
            }
            grandTotal += rowTotal;
            rowHtml.append("<td style=\"text-align:center;font-weight:bold\">").append(rowTotal).append("</td>");
            if (isProcent == 1) rowHtml.append("<td></td>");
            rowHtml.append("</tr>\n");
            html.append(rowHtml);
        }

        // Строка "Итого"
        html.append("<tr style=\"background:#f0f0f0;font-weight:bold\">");
        if (drPp == 1) html.append("<td></td>");
        html.append("<td>Итого</td>");
        for (String ck : cols) {
            html.append("<td style=\"text-align:center\">")
                    .append(colTotals.getOrDefault(ck, 0))
                    .append("</td>");
        }
        html.append("<td style=\"text-align:center\">").append(grandTotal).append("</td>");
        if (isProcent == 1) html.append("<td>100%</td>");
        html.append("</tr>\n");

        html.append("</tbody></table>\n");
        return html.toString();
    }

    // ─── gen_table_select ─────────────────────────────────────────────────────

    /**
     * Аналог gen_table_select.
     * Выполняет SQL и генерирует HTML-таблицу.
     *
     * @param sql        SELECT-запрос
     * @param headers    Map заголовков: ключ=номер колонки (1..N), значение=заголовок
     * @param repBeg     текст над таблицей
     * @param cntCol     количество колонок (null = авто)
     * @param calcTotal  считать итоги по числовым колонкам
     * @param drPp       рисовать № п/п
     * @return HTML-строка
     */
    public String genTableSelect(String sql,
                                 Map<Integer, String> headers,
                                 String repBeg,
                                 Integer cntCol,
                                 boolean calcTotal,
                                 int drPp) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) {
            return "<p><i>Данные отсутствуют</i></p>";
        }

        // Получаем порядок колонок из первой строки
        List<String> colOrder = new ArrayList<>(rows.get(0).keySet());
        int totalCols = (cntCol != null) ? Math.min(cntCol, colOrder.size()) : colOrder.size();

        StringBuilder html = new StringBuilder();
        if (repBeg != null && !repBeg.isEmpty()) {
            html.append("<p><b>").append(repBeg).append("</b></p>\n");
        }

        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\">\n");

        // Заголовок
        html.append("<thead><tr style=\"background:#d0e4f7\">");
        if (drPp == 1) html.append("<th>№</th>");
        for (int i = 0; i < totalCols; i++) {
            String h = (headers != null && headers.containsKey(i + 1))
                    ? headers.get(i + 1)
                    : colOrder.get(i);
            html.append("<th>").append(repl(h)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        // Данные + подсчёт итогов
        double[] totals = new double[totalCols];
        boolean[] isNumeric = new boolean[totalCols];
        Arrays.fill(isNumeric, true);
        int ppNum = 0;

        for (Map<String, Object> row : rows) {
            html.append("<tr>");
            if (drPp == 1) html.append("<td>").append(++ppNum).append("</td>");
            for (int i = 0; i < totalCols; i++) {
                Object val = row.get(colOrder.get(i));
                String cell = (val != null) ? val.toString() : "";
                html.append("<td>").append(repl(cell)).append("</td>");
                if (calcTotal) {
                    try {
                        totals[i] += Double.parseDouble(cell);
                    } catch (NumberFormatException e) {
                        isNumeric[i] = false;
                    }
                }
            }
            html.append("</tr>\n");
        }

        // Строка итогов
        if (calcTotal) {
            html.append("<tr style=\"background:#f0f0f0;font-weight:bold\">");
            if (drPp == 1) html.append("<td></td>");
            for (int i = 0; i < totalCols; i++) {
                if (i == 0) { html.append("<td>Итого</td>"); continue; }
                if (isNumeric[i]) {
                    String total = (totals[i] == Math.floor(totals[i]))
                            ? String.valueOf((long) totals[i])
                            : String.valueOf(totals[i]);
                    html.append("<td style=\"text-align:center\">").append(total).append("</td>");
                } else {
                    html.append("<td></td>");
                }
            }
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n");
        return html.toString();
    }

    // ─── dr_table ─────────────────────────────────────────────────────────────

    /**
     * Аналог dr_table.
     * Отрисовка таблицы из Tab_Array_List (уже заполненного Map).
     *
     * @param tabWork    Map<String, String[]> — x1..x30 значения
     * @param headers    заголовки колонок (1-based)
     * @param repBeg     текст над таблицей
     * @param cntCol     количество колонок
     * @param displayCols список номеров колонок для отображения (null = все)
     * @param drPp       рисовать № п/п
     * @return HTML
     */
    public String drTable(Map<String, String[]> tabWork,
                          Map<Integer, String> headers,
                          String repBeg,
                          int cntCol,
                          List<Integer> displayCols,
                          int drPp) {

        if (tabWork == null || tabWork.isEmpty()) {
            return "<p><i>Данные отсутствуют</i></p>";
        }

        // Определяем какие колонки показывать
        List<Integer> cols = new ArrayList<>();
        if (displayCols != null && !displayCols.isEmpty()) {
            cols.addAll(displayCols);
        } else {
            for (int i = 1; i <= cntCol; i++) cols.add(i);
        }

        StringBuilder html = new StringBuilder();
        if (repBeg != null && !repBeg.isEmpty()) {
            html.append("<p><b>").append(repBeg).append("</b></p>\n");
        }

        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\">\n");

        // Заголовок
        html.append("<thead><tr style=\"background:#d0e4f7\">");
        if (drPp == 1) html.append("<th>№</th>");
        for (int c : cols) {
            String h = (headers != null && headers.containsKey(c)) ? headers.get(c) : "Колонка " + c;
            html.append("<th>").append(repl(h)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        // Сортируем ключи
        List<String> keys = new ArrayList<>(tabWork.keySet());
        Collections.sort(keys);
        int ppNum = 0;

        for (String key : keys) {
            String[] values = tabWork.get(key);
            html.append("<tr>");
            if (drPp == 1) html.append("<td>").append(++ppNum).append("</td>");
            for (int c : cols) {
                String val = (values != null && c <= values.length) ? values[c - 1] : "";
                html.append("<td>").append(repl(val != null ? val : "")).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n");
        return html.toString();
    }

    // ─── Утилиты времени (аналог end_time_report) ─────────────────────────────

    public String endTimeReport(java.time.LocalDateTime datBegin, java.time.LocalDateTime datEnd) {
        if (datEnd == null) datEnd = java.time.LocalDateTime.now();
        long seconds = java.time.Duration.between(datBegin, datEnd).getSeconds();
        return String.format("<p style=\"color:gray;font-size:0.85em\">Время формирования: %d сек.</p>", seconds);
    }

    // ─── Конструктор ──────────────────────────────────────────────────────────

    public XrepService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}