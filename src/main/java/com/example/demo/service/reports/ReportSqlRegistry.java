package com.example.demo.service.reports;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реестр SQL-запросов для отчётов.
 * D_ACTION_ID → SQL + заголовки + название
 *
 * Параметры которые могут прийти из lev1 маски:
 *   payment  — вид выплаты (07, 0701, 0702, 0703, 0704, 0705)
 *   cnt_day  — количество дней (для отчётов с порогом)
 *   bdat     — дата с (yyyy-MM-dd)
 *   edat     — дата по (yyyy-MM-dd)
 */
@Component
public class ReportSqlRegistry {

    public static class ReportDefinition {
        public final String               sql;
        public final Map<Integer, String> headers;
        public final String               title;

        public ReportDefinition(String title, String sql, Map<Integer, String> headers) {
            this.title   = title;
            this.sql     = sql;
            this.headers = headers;
        }
    }

    // ─── Старый метод (совместимость) ─────────────────────────────────────────
    public ReportDefinition get(Long repId, String begDate, String endDate) {
        return get(repId, begDate, endDate, null, null);
    }

    // ─── Основной метод со всеми параметрами ─────────────────────────────────
    public ReportDefinition get(Long repId, String begDate, String endDate,
                                String payment, String cntDay) {
        if (repId == null) return null;

        String bdat    = sanitizeDate(begDate);
        String edat    = sanitizeDate(endDate);
        // payment: "07" = все, "0701".."0705" = конкретный вид
        String pc      = sanitizeAlphaNum(payment);
        // cnt_day: порог в днях
        int    days    = parseDays(cntDay, 2);  // по умолчанию 2 дня

        switch (repId.intValue()) {

            // ── Группа (20-29) Нарушение сроков ──────────────────────────────
            case 17521: return report17521(pc, days);
            case 17522: return report17522(pc, days);
            case 17523: return report17523(pc, days);
            case 17524: return report17524(pc, days);
            case 17525: return report17525(pc, days);
            case 17526: return report17526(pc, days);
            case 17527: return report17527(pc, days);
            case 17528: return report17528(pc, days);
            case 17529: return report17529(pc, days);

            // ── Группа (98-104) Приостановление ──────────────────────────────
            case 17647: return report17647(bdat, edat);
            case 17651: return report17651(bdat, edat);
            case 17658: return report17658(bdat, edat);
            case 17828: return report17828(bdat, edat);

            // ── Группа (150-155) Заявления ────────────────────────────────────
            case 17842: return report17842(bdat, edat);
            case 17843: return report17843(bdat, edat);

            // ── Группа (97) Оказанные услуги ─────────────────────────────────
            case 17597: return report17597(bdat, edat);
            case 17838: return report17838(bdat, edat);

            // ── Ошибки m_eg ───────────────────────────────────────────────────
            case 17990: return report17990(bdat, edat);
            case 17991: return report17991(bdat, edat);

            // ── Реестр выплат ─────────────────────────────────────────────────
            case 17997: return report17997(bdat, edat);

            default: return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ГРУППА (20-29): Нарушение сроков
    // Все эти отчёты НЕ используют диапазон дат — они показывают текущее
    // состояние (просрочено прямо сейчас). Фильтр — только вид выплаты (pc).
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * (21) Список просроченных макетов > N рабочих дней
     * от "Даты обращения" до "Даты отправки в филиал Центра"
     * Oracle CMD: SSRep_Prosroch_21  |  D_ACTION_ID: 17521
     *
     * Параметры из lev1 маски:
     *   payment — вид выплаты (07 = все)
     *
     * Статусы "в работе у отделения" (ещё не отправлен в центр):
     *   st2 IN (0,1,13,61,71,81,92,100) — активные, без отката
     *
     * Основания id_osn:
     *   101=по потере кормильца, 102=по утрате трудоспособности,
     *   103=по потере работы, 104=по беременности и родам,
     *   105=по уходу за ребёнком
     */
    private ReportDefinition report17521(String pc, int days) {
        // Формируем условие по виду выплаты
        // payment="07" означает ВСЕ соц.выплаты (07xx)
        // payment="0703" означает конкретный вид
        String pcCondition = buildPcCondition(pc);

        return new ReportDefinition(
                "(21) Список просроченных макетов > " + days + " рабочих дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn)
                                                            AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln)  || ' ' ||
                    INITCAP(eg.fn)  || ' ' ||
                    INITCAP(COALESCE(eg.mn, ''))            AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(pay.d_naz,  'dd.mm.yyyy')       AS "Дата назначения",
                    (CURRENT_DATE - z.d_inp) - %d           AS "Кол-во просроченных дней",
                    sol.st                                  AS "Статус",
                    z.id_tip                                AS "Тип назначения",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата изменения статуса",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay
                    ON pay.id = sol.mpay
                JOIN em5.z_doc z
                    ON z.id = sol.id
                JOIN em5.m_eg eg
                    ON eg.id = z.id_eg_
                -- Последний действующий статус "в работе у отделения"
                -- (не откатан, backid IS NULL)
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id)
                        FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (0, 1, 13, 61, 71, 81, 92, 100)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (
                    0,   1,   8,   13,  30,  31,
                    60,  61,  62,  69,  71,  72,
                    80,  81,  82,  89,  92,  93,
                    100, 101
                )
                  AND (CURRENT_DATE - z.d_inp) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn, eg.mn
                """,
                        days,   // для вычисления "Кол-во просроченных дней"
                        days,   // для фильтра > N дней
                        pcCondition
                ),
                headers(
                        "№п/п", "Код отделения", "Вид выплаты", "№ дела",
                        "ИИН", "ФИО", "Дата рождения", "Дата назначения",
                        "Кол-во просроченных дней", "Статус", "Тип назначения",
                        "Дата изменения статуса", "Инспектор"
                )
        );
    }

    /**
     * (22) Просрочка от "Отправки в филиал Центра" до "Отправки в Департамент" > N дней
     * Oracle CMD: SSRep_Prosroch_22  |  D_ACTION_ID: 17522
     * Статусы "в центре/филиале, ещё не в департаменте": st2 IN (30,31,60,62,72,82,89,93,101)
     */
    private ReportDefinition report17522(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(22) Просрочка от отправки в филиал Центра до Департамента > " + days + " дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(pay.d_naz,  'dd.mm.yyyy')       AS "Дата назначения",
                    CURRENT_DATE - sol_st.dat               AS "Дней в текущем статусе",
                    sol.st                                  AS "Статус",
                    z.id_tip                                AS "Тип назначения",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата изменения статуса",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (30, 31, 60, 62, 72, 82, 89, 93, 101)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (30,31,60,62,69,72,82,89,93,101)
                  AND (CURRENT_DATE - sol_st.dat) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения","Дата назначения",
                        "Дней в текущем статусе","Статус","Тип назначения",
                        "Дата изменения статуса","Инспектор")
        );
    }

    /** (23) Просрочка от Департамента до Утверждения > N дней | D_ACTION_ID: 17523 */
    private ReportDefinition report17523(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(23) Просрочка от Департамента до Утверждения > " + days + " дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(pay.d_naz,  'dd.mm.yyyy')       AS "Дата назначения",
                    CURRENT_DATE - sol_st.dat               AS "Дней в текущем статусе",
                    sol.st                                  AS "Статус",
                    z.id_tip                                AS "Тип назначения",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата изменения статуса",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (8, 69, 80, 81)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (8, 69, 80, 81)
                  AND (CURRENT_DATE - sol_st.dat) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения","Дата назначения",
                        "Дней в текущем статусе","Статус","Тип назначения",
                        "Дата изменения статуса","Инспектор")
        );
    }

    /** (24) Просрочка от Утверждения до Переноса на выплату > N дней | D_ACTION_ID: 17524 */
    private ReportDefinition report17524(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(24) Просрочка от Утверждения до Переноса на выплату > " + days + " дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(pay.d_naz,  'dd.mm.yyyy')       AS "Дата назначения",
                    CURRENT_DATE - sol_st.dat               AS "Дней с утверждения",
                    sol.st                                  AS "Статус",
                    z.id_tip                                AS "Тип назначения",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата изменения статуса",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (90, 92)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (90, 92)
                  AND (CURRENT_DATE - sol_st.dat) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения","Дата назначения",
                        "Дней с утверждения","Статус","Тип назначения",
                        "Дата изменения статуса","Инспектор")
        );
    }

    /** (25) Просрочка от Отправки на доработку до Утверждения > N дней | D_ACTION_ID: 17525 */
    private ReportDefinition report17525(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(25) Просрочка от Отправки на доработку до Утверждения > " + days + " дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    CURRENT_DATE - sol_st.dat               AS "Дней на доработке",
                    sol.st                                  AS "Статус",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата отправки на доработку",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (13, 61, 71, 81, 92)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (13, 61, 71, 81, 92)
                  AND (CURRENT_DATE - sol_st.dat) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения",
                        "Дней на доработке","Статус",
                        "Дата отправки на доработку","Инспектор")
        );
    }

    /** (26) Угроза просрочки — дней до срока <= N | D_ACTION_ID: 17526 */
    private ReportDefinition report17526(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(26) Угроза просрочки: осталось <= " + days + " дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.est_date, z.brid) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(z.d_inp,    'dd.mm.yyyy')       AS "Дата обращения",
                    to_char(z.est_date, 'dd.mm.yyyy')       AS "Срок оказания услуги",
                    z.est_date - CURRENT_DATE               AS "Дней до просрочки",
                    sol.st                                  AS "Статус"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                WHERE sol.st IN (0,1,8,13,30,31,60,61,62,69,71,72,80,81,82,89,92,93,100,101)
                  AND z.est_date BETWEEN CURRENT_DATE AND CURRENT_DATE + %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.est_date, z.brid
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения","Дата обращения",
                        "Срок оказания услуги","Дней до просрочки","Статус")
        );
    }

    /** (27) Просрочка от Доработки/Проверки до Утверждения > N дней | D_ACTION_ID: 17527 */
    private ReportDefinition report17527(String pc, int days) {
        // Аналог 17525 но включает статусы проверки
        return report17525(pc, days);
    }

    /** (28) Просрочка от "На проверку" > N календарных дней | D_ACTION_ID: 17528 */
    private ReportDefinition report17528(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        return new ReportDefinition(
                "(28) Просрочка от отправки «На проверку» > " + days + " календарных дней",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    CURRENT_DATE - sol_st.dat               AS "Дней на проверке",
                    sol.st                                  AS "Статус",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата отправки на проверку",
                    sol_st.usr                              AS "Инспектор"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (43, 44, 45)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (43, 44, 45)
                  AND (CURRENT_DATE - sol_st.dat) > %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY z.brid, eg.ln, eg.fn
                """, days, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения",
                        "Дней на проверке","Статус",
                        "Дата отправки на проверку","Инспектор")
        );
    }

    /** (29-1) Угроза просрочки >= N дней от даты обращения | D_ACTION_ID: 17529 */
    private ReportDefinition report17529(String pc, int days) {
        String pcCondition = buildPcCondition(pc);
        int threshold = days > 0 ? days : 2;
        return new ReportDefinition(
                "(29-1) Угроза просрочки >= " + threshold + " дней от даты обращения",
                String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY z.brid, eg.ln, eg.fn) AS "№п/п",
                    z.brid                                  AS "Код отделения",
                    pay.pc                                  AS "Вид выплаты",
                    sol.numb                                AS "№ дела",
                    eg.iin                                  AS "ИИН",
                    INITCAP(eg.ln) || ' ' || INITCAP(eg.fn) || ' ' ||
                    INITCAP(COALESCE(eg.mn,''))             AS "ФИО",
                    to_char(eg.bd,      'dd.mm.yyyy')       AS "Дата рождения",
                    to_char(z.d_inp,    'dd.mm.yyyy')       AS "Дата обращения",
                    CURRENT_DATE - z.d_inp                  AS "Дней от обращения",
                    sol.st                                  AS "Статус",
                    to_char(sol_st.dat, 'dd.mm.yyyy')       AS "Дата изменения статуса"
                FROM em5.m_sol sol
                JOIN em5.m_pay pay ON pay.id = sol.mpay
                JOIN em5.z_doc z ON z.id = sol.id
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol_st sol_st
                    ON sol_st.id = (
                        SELECT MAX(xt.id) FROM em5.m_sol_st xt
                        WHERE xt.sid = sol.id
                          AND xt.st2 IN (0, 1, 13, 61, 71, 81, 92, 100)
                          AND xt.backid IS NULL
                          AND NOT EXISTS (
                              SELECT 1 FROM em5.m_sol_st e
                              WHERE e.backid = xt.id AND e.sid = xt.sid
                          )
                    )
                WHERE sol.st IN (0,1,8,13,30,31,60,61,62,69,71,72,80,81,82,89,92,93,100,101)
                  AND (CURRENT_DATE - z.d_inp) >= %d
                  AND z.id_osn IN (101, 102, 103, 104, 105)
                  %s
                ORDER BY (CURRENT_DATE - z.d_inp) DESC, z.brid
                """, threshold, pcCondition),
                headers("№п/п","Код отделения","Вид выплаты","№ дела",
                        "ИИН","ФИО","Дата рождения","Дата обращения",
                        "Дней от обращения","Статус","Дата изменения статуса")
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ОСТАЛЬНЫЕ ОТЧЁТЫ (с диапазоном дат)
    // ═════════════════════════════════════════════════════════════════════════

    private ReportDefinition report17647(String bdat, String edat) {
        return new ReportDefinition(
                "(98-2) Список лиц по пересмотренным делам",
                String.format("""
                SELECT eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    to_char(eg.bd,'dd.mm.yyyy')   AS "Дата рождения",
                    z.brid                         AS "Отделение",
                    z.num                          AS "№ заявления",
                    z.id_osn                       AS "Основание",
                    sol.n_resh                     AS "№ решения",
                    to_char(sol.d_resh,'dd.mm.yyyy') AS "Дата решения",
                    pay.nsum                       AS "Сумма",
                    to_char(pay.d_naz,'dd.mm.yyyy') AS "Дата назначения"
                FROM em5.z_doc z
                JOIN em5.m_eg eg  ON eg.id  = z.id_eg_
                JOIN em5.m_sol sol ON sol.id = z.id
                JOIN em5.m_pay pay ON pay.sid = sol.id
                WHERE z.id_tip = 'REC' AND z.d_inp BETWEEN '%s' AND '%s'
                ORDER BY z.brid, z.d_inp
                """, bdat, edat),
                headers("ИИН","ФИО","Дата рождения","Отделение","№ заявления",
                        "Основание","№ решения","Дата решения","Сумма","Дата назначения")
        );
    }

    private ReportDefinition report17651(String bdat, String edat) {
        return new ReportDefinition(
                "(99-1) Количество отказанных дел по источникам",
                String.format("""
                SELECT z.id_sour_type AS "Тип источника", z.id_sour AS "Источник",
                    COUNT(*) AS "Количество отказов"
                FROM em5.z_doc z
                WHERE z.is_otkaz = 1 AND z.d_inp BETWEEN '%s' AND '%s'
                GROUP BY z.id_sour_type, z.id_sour ORDER BY COUNT(*) DESC
                """, bdat, edat),
                headers("Тип источника","Источник","Количество отказов")
        );
    }

    private ReportDefinition report17658(String bdat, String edat) {
        return new ReportDefinition(
                "(99-3) Список лиц по отказанным делам",
                String.format("""
                SELECT eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    to_char(eg.bd,'dd.mm.yyyy')     AS "Дата рождения",
                    z.brid                           AS "Отделение",
                    z.num                            AS "№ заявления",
                    to_char(z.d_inp,'dd.mm.yyyy')   AS "Дата обращения",
                    to_char(z.d_reg,'dd.mm.yyyy')   AS "Дата регистрации",
                    z.id_osn                         AS "Основание",
                    sol.n_resh                       AS "№ решения",
                    to_char(sol.d_resh,'dd.mm.yyyy') AS "Дата решения"
                FROM em5.z_doc z
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                LEFT JOIN em5.m_sol sol ON sol.id = z.id
                WHERE z.is_otkaz = 1 AND z.d_inp BETWEEN '%s' AND '%s'
                ORDER BY z.brid, z.d_inp
                """, bdat, edat),
                headers("ИИН","ФИО","Дата рождения","Отделение","№ заявления",
                        "Дата обращения","Дата регистрации","Основание","№ решения","Дата решения")
        );
    }

    private ReportDefinition report17828(String bdat, String edat) {
        return new ReportDefinition(
                "(103-2) Список лиц по прекращённым делам",
                String.format("""
                SELECT eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    to_char(eg.bd,'dd.mm.yyyy')        AS "Дата рождения",
                    z.brid                              AS "Отделение",
                    z.num                               AS "№ заявления",
                    z.id_osn                            AS "Основание",
                    to_char(pay.d_naz,'dd.mm.yyyy')    AS "Дата назначения",
                    to_char(pay.stopdate,'dd.mm.yyyy')  AS "Дата прекращения",
                    pay.pc                              AS "Способ выплаты",
                    pay.nsum                            AS "Сумма"
                FROM em5.z_doc z
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol sol ON sol.id = z.id
                JOIN em5.m_pay pay ON pay.sid = sol.id
                WHERE pay.stopdate BETWEEN '%s' AND '%s' AND pay.closeact IS NOT NULL
                ORDER BY z.brid, pay.stopdate
                """, bdat, edat),
                headers("ИИН","ФИО","Дата рождения","Отделение","№ заявления",
                        "Основание","Дата назначения","Дата прекращения","Способ выплаты","Сумма")
        );
    }

    private ReportDefinition report17842(String bdat, String edat) {
        return new ReportDefinition(
                "(151) Количество зарегистрированных / незарегистрированных заявлений",
                String.format("""
                SELECT z.brid AS "Отделение", z.id_sour_type AS "Тип источника",
                    z.id_sour AS "Источник", COUNT(*) AS "Всего",
                    SUM(CASE WHEN z.idsol IS NOT NULL THEN 1 ELSE 0 END) AS "Зарегистрировано",
                    SUM(CASE WHEN z.idsol IS NULL THEN 1 ELSE 0 END) AS "Не зарегистрировано"
                FROM em5.z_doc z WHERE z.d_inp BETWEEN '%s' AND '%s'
                GROUP BY z.brid, z.id_sour_type, z.id_sour ORDER BY z.brid
                """, bdat, edat),
                headers("Отделение","Тип источника","Источник","Всего","Зарегистрировано","Не зарегистрировано")
        );
    }

    private ReportDefinition report17843(String bdat, String edat) {
        return new ReportDefinition(
                "(152) Список зарегистрированных / незарегистрированных заявлений",
                String.format("""
                SELECT eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    z.brid AS "Отделение", z.num AS "№ заявления",
                    to_char(z.d_inp,'dd.mm.yyyy') AS "Дата обращения",
                    z.id_sour_type AS "Тип источника", z.id_sour AS "Источник",
                    z.id_osn AS "Основание",
                    CASE WHEN z.idsol IS NOT NULL THEN 'Да' ELSE 'Нет' END AS "Зарегистрировано"
                FROM em5.z_doc z JOIN em5.m_eg eg ON eg.id = z.id_eg_
                WHERE z.d_inp BETWEEN '%s' AND '%s' ORDER BY z.d_inp, z.brid
                """, bdat, edat),
                headers("ИИН","ФИО","Отделение","№ заявления","Дата обращения",
                        "Тип источника","Источник","Основание","Зарегистрировано")
        );
    }

    private ReportDefinition report17597(String bdat, String edat) {
        return new ReportDefinition(
                "(97-1) Сведения об оказанных услугах",
                String.format("""
                SELECT z.brid AS "Отделение", z.id_osn AS "Основание",
                    COUNT(*) AS "Кол-во заявлений",
                    SUM(CASE WHEN z.is_otkaz=0 THEN 1 ELSE 0 END) AS "Назначено",
                    SUM(CASE WHEN z.is_otkaz=1 THEN 1 ELSE 0 END) AS "Отказано",
                    ROUND(AVG(CURRENT_DATE - z.d_inp),1) AS "Среднее время (дней)"
                FROM em5.z_doc z WHERE z.d_reg BETWEEN '%s' AND '%s'
                GROUP BY z.brid, z.id_osn ORDER BY z.brid, z.id_osn
                """, bdat, edat),
                headers("Отделение","Основание","Кол-во заявлений","Назначено","Отказано","Среднее время (дней)")
        );
    }

    private ReportDefinition report17838(String bdat, String edat) {
        return new ReportDefinition(
                "(110) Количество поступивших заявлений по проактивной услуге",
                String.format("""
                SELECT z.brid AS "Отделение", z.id_osn AS "Основание",
                    z.id_sour_type AS "Тип источника", COUNT(*) AS "Кол-во заявлений"
                FROM em5.z_doc z WHERE z.d_inp BETWEEN '%s' AND '%s' AND z.id_sour_type='PRO'
                GROUP BY z.brid, z.id_osn, z.id_sour_type ORDER BY z.brid
                """, bdat, edat),
                headers("Отделение","Основание","Тип источника","Кол-во заявлений")
        );
    }

    private ReportDefinition report17990(String bdat, String edat) {
        return new ReportDefinition(
                "Заявки со статусом 12 (ошибка)",
                String.format("""
                SELECT eg.id AS "ID", eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    eg.brid AS "Отделение",
                    to_char(eg.d_reg,'dd.mm.yyyy') AS "Дата регистрации",
                    eg.id_sour_type AS "Тип источника", eg.comm AS "Комментарий"
                FROM em5.m_eg eg WHERE eg.id_st=12 AND eg.d_reg BETWEEN '%s' AND '%s'
                ORDER BY eg.d_reg DESC
                """, bdat, edat),
                headers("ID","ИИН","ФИО","Отделение","Дата регистрации","Тип источника","Комментарий")
        );
    }

    private ReportDefinition report17991(String bdat, String edat) {
        return new ReportDefinition(
                "Ошибки в электронных заявках (m_eg)",
                String.format("""
                SELECT eg.id AS "ID", eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    eg.brid AS "Отделение",
                    to_char(eg.d_reg,'dd.mm.yyyy') AS "Дата регистрации",
                    eg.id_sour AS "Источник", eg.id_sour_type AS "Тип источника",
                    eg.comm AS "Ошибка / комментарий"
                FROM em5.m_eg eg WHERE eg.id_st IN (12,99) AND eg.d_reg BETWEEN '%s' AND '%s'
                ORDER BY eg.d_reg DESC
                """, bdat, edat),
                headers("ID","ИИН","ФИО","Отделение","Дата регистрации","Источник","Тип источника","Ошибка / комментарий")
        );
    }

    private ReportDefinition report17997(String bdat, String edat) {
        return new ReportDefinition(
                "Реестр назначенных выплат",
                String.format("""
                SELECT eg.iin AS "ИИН",
                    eg.ln || ' ' || eg.fn || ' ' || COALESCE(eg.mn,'') AS "ФИО",
                    to_char(eg.bd,'dd.mm.yyyy') AS "Дата рождения",
                    z.brid AS "Отделение", z.num AS "№ заявления", z.id_osn AS "Основание",
                    pay.pc AS "Способ выплаты",
                    to_char(pay.d_naz,'dd.mm.yyyy')    AS "Дата назначения",
                    to_char(pay.stopdate,'dd.mm.yyyy')  AS "Дата окончания",
                    pay.nsum AS "Сумма", pay.status AS "Статус выплаты"
                FROM em5.z_doc z
                JOIN em5.m_eg eg ON eg.id = z.id_eg_
                JOIN em5.m_sol sol ON sol.id = z.id
                JOIN em5.m_pay pay ON pay.sid = sol.id
                WHERE pay.d_naz BETWEEN '%s' AND '%s' ORDER BY pay.d_naz, z.brid
                """, bdat, edat),
                headers("ИИН","ФИО","Дата рождения","Отделение","№ заявления","Основание",
                        "Способ выплаты","Дата назначения","Дата окончания","Сумма","Статус выплаты")
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Вспомогательные методы
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Строит условие по виду выплаты (pc).
     * payment="07"   → AND pay.pc LIKE '07%'  (все соц.выплаты)
     * payment="0703" → AND pay.pc = '0703'    (конкретный вид)
     * payment=null   → (без фильтра)
     */
    /**
     * Строит условие фильтра по виду выплаты (pc).
     *
     * В БД pc хранится в полном формате: 07030106, 07040101 и т.д.
     * Поэтому всегда используем LIKE с префиксом:
     *   "07"   → LIKE '07%'   (все соц. выплаты)
     *   "0703" → LIKE '0703%' (По потере работы)
     *   "0704" → LIKE '0704%' (По беременности и родам)
     *   и т.д.
     */
    private String buildPcCondition(String pc) {
        if (pc == null || pc.isBlank()) return "";
        return "AND pay.pc LIKE '" + pc + "%'";
    }

    private Map<Integer, String> headers(String... titles) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < titles.length; i++) map.put(i + 1, titles[i]);
        return map;
    }

    private String sanitizeDate(String date) {
        if (date == null || date.isBlank()) return "1900-01-01";
        return date.replaceAll("[^0-9\\-]", "");
    }

    private String sanitizeAlphaNum(String val) {
        if (val == null || val.isBlank()) return null;
        return val.replaceAll("[^0-9a-zA-Z]", "");
    }

    private int parseDays(String val, int defaultVal) {
        try { return Integer.parseInt(val); } catch (Exception e) { return defaultVal; }
    }
}