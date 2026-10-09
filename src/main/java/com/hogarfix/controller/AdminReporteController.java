package com.hogarfix.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hogarfix.model.Pago;
import com.hogarfix.model.Servicio;
import com.hogarfix.repository.PagoRepository;
import com.hogarfix.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reporte mensual: servicios solicitados y atendidos, técnicos que atendieron
 * y monto cobrado, desglosado por especialidad. Se puede filtrar por
 * especialidad y exportar a CSV o Excel.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    private final ServicioRepository servicioRepository;
    private final PagoRepository pagoRepository;

    public record Fila(String especialidad, long solicitados, long atendidos, long finalizados,
            long tecnicos, BigDecimal monto) {
    }

    private record Reporte(YearMonth periodo, List<Fila> filas, long totSolicitados, long totAtendidos,
            long totFinalizados, int totTecnicos, BigDecimal montoTotal) {
    }

    @GetMapping
    public String ver(@RequestParam(required = false) String mes,
            @RequestParam(required = false) String q,
            Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        Reporte r = calcular(mes, q);
        String nombreMes = r.periodo().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        model.addAttribute("mes", r.periodo().toString());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("mesTexto", nombreMes + " " + r.periodo().getYear());
        model.addAttribute("filas", r.filas());
        model.addAttribute("totSolicitados", r.totSolicitados());
        model.addAttribute("totAtendidos", r.totAtendidos());
        model.addAttribute("totFinalizados", r.totFinalizados());
        model.addAttribute("totTecnicos", r.totTecnicos());
        model.addAttribute("montoTotal", r.montoTotal());
        return "admin/reportes";
    }

    // ---------------- Exportaciones ----------------

    @GetMapping("/exportar.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) String mes,
            @RequestParam(required = false) String q) {
        Reporte r = calcular(mes, q);
        StringBuilder sb = new StringBuilder();
        fila(sb, "Especialidad", "Solicitados", "Atendidos", "Finalizados", "Tecnicos", "Monto (S/)");
        for (Fila f : r.filas()) {
            fila(sb, f.especialidad(), String.valueOf(f.solicitados()), String.valueOf(f.atendidos()),
                    String.valueOf(f.finalizados()), String.valueOf(f.tecnicos()), f.monto().toPlainString());
        }
        fila(sb, "TOTAL", String.valueOf(r.totSolicitados()), String.valueOf(r.totAtendidos()),
                String.valueOf(r.totFinalizados()), String.valueOf(r.totTecnicos()),
                r.montoTotal().toPlainString());

        // BOM UTF-8 para que Excel muestre bien tildes y ñ
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] cuerpo = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] salida = new byte[bom.length + cuerpo.length];
        System.arraycopy(bom, 0, salida, 0, bom.length);
        System.arraycopy(cuerpo, 0, salida, bom.length, cuerpo.length);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporte_" + r.periodo() + ".csv", StandardCharsets.UTF_8).build().toString())
                .body(salida);
    }

    @GetMapping("/exportar.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> exportarExcel(@RequestParam(required = false) String mes,
            @RequestParam(required = false) String q) throws IOException {
        Reporte r = calcular(mes, q);
        String[] encabezados = { "Especialidad", "Solicitados", "Atendidos", "Finalizados", "Técnicos", "Monto (S/)" };

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reporte " + r.periodo());

            Font blanca = wb.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());
            CellStyle estiloEnc = wb.createCellStyle();
            estiloEnc.setFont(blanca);
            estiloEnc.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            estiloEnc.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font negrita = wb.createFont();
            negrita.setBold(true);
            CellStyle estiloTotal = wb.createCellStyle();
            estiloTotal.setFont(negrita);

            Row enc = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                Cell c = enc.createCell(i);
                c.setCellValue(encabezados[i]);
                c.setCellStyle(estiloEnc);
            }
            sheet.createFreezePane(0, 1);

            int n = 1;
            for (Fila f : r.filas()) {
                Row row = sheet.createRow(n++);
                row.createCell(0).setCellValue(f.especialidad());
                row.createCell(1).setCellValue(f.solicitados());
                row.createCell(2).setCellValue(f.atendidos());
                row.createCell(3).setCellValue(f.finalizados());
                row.createCell(4).setCellValue(f.tecnicos());
                row.createCell(5).setCellValue(f.monto().doubleValue());
            }
            Row total = sheet.createRow(n);
            Object[] valores = { "TOTAL", r.totSolicitados(), r.totAtendidos(), r.totFinalizados(),
                    r.totTecnicos(), r.montoTotal().doubleValue() };
            for (int i = 0; i < valores.length; i++) {
                Cell c = total.createCell(i);
                if (valores[i] instanceof Number num) {
                    c.setCellValue(num.doubleValue());
                } else {
                    c.setCellValue(String.valueOf(valores[i]));
                }
                c.setCellStyle(estiloTotal);
            }
            for (int i = 0; i < encabezados.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("reporte_" + r.periodo() + ".xlsx", StandardCharsets.UTF_8).build().toString())
                    .body(out.toByteArray());
        }
    }

    // ---------------- Cálculo ----------------

    private Reporte calcular(String mes, String q) {
        YearMonth periodo;
        try {
            periodo = (mes == null || mes.isBlank()) ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException ex) {
            periodo = YearMonth.now();
        }
        LocalDateTime desde = periodo.atDay(1).atStartOfDay();
        LocalDateTime hasta = periodo.plusMonths(1).atDay(1).atStartOfDay();
        String filtro = normalizar(q);

        List<Servicio> servicios = servicioRepository.findDelPeriodo(desde, hasta);
        List<Pago> pagos = pagoRepository.findPagadosDelPeriodo(desde, hasta);

        // [0]=solicitados, [1]=atendidos (aceptados), [2]=finalizados
        Map<String, long[]> conteos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<Long>> tecnicosPorEsp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, BigDecimal> montos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<Long> tecnicosTotal = new HashSet<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (Servicio s : servicios) {
            String esp = s.getCategoria().getNombre();
            if (!normalizar(esp).contains(filtro)) {
                continue;
            }
            long[] c = conteos.computeIfAbsent(esp, k -> new long[3]);
            c[0]++;
            String estado = s.getEstado() == null ? "" : s.getEstado();
            if (estado.equals("ACEPTADO") || estado.equals("EN_CAMINO") || estado.equals("EN_PROCESO")
                    || estado.equals("FINALIZADO")) {
                c[1]++;
                if (s.getTecnico() != null) {
                    tecnicosPorEsp.computeIfAbsent(esp, k -> new HashSet<>()).add(s.getTecnico().getIdTecnico());
                    tecnicosTotal.add(s.getTecnico().getIdTecnico());
                }
            }
            if (estado.equals("FINALIZADO")) {
                c[2]++;
            }
        }

        for (Pago p : pagos) {
            String esp = p.getServicio().getCategoria().getNombre();
            if (!normalizar(esp).contains(filtro)) {
                continue;
            }
            montos.merge(esp, p.getMonto(), BigDecimal::add);
            montoTotal = montoTotal.add(p.getMonto());
        }

        Set<String> especialidades = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        especialidades.addAll(conteos.keySet());
        especialidades.addAll(montos.keySet());

        List<Fila> filas = new ArrayList<>();
        long totSol = 0, totAt = 0, totFin = 0;
        for (String esp : especialidades) {
            long[] c = conteos.getOrDefault(esp, new long[3]);
            filas.add(new Fila(esp, c[0], c[1], c[2],
                    tecnicosPorEsp.getOrDefault(esp, Set.of()).size(),
                    montos.getOrDefault(esp, BigDecimal.ZERO)));
            totSol += c[0];
            totAt += c[1];
            totFin += c[2];
        }
        return new Reporte(periodo, filas, totSol, totAt, totFin, tecnicosTotal.size(), montoTotal);
    }

    /** Minúsculas y sin tildes, para que "electrodomesticos" encuentre "Electrodomésticos". */
    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static void fila(StringBuilder sb, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String v = valores[i] == null ? "" : valores[i];
            boolean comillas = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
            sb.append(comillas ? "\"" + v.replace("\"", "\"\"") + "\"" : v);
        }
        sb.append("\r\n");
    }
}
