package com.hogarfix.controller;

import com.hogarfix.model.Tecnico;
import com.hogarfix.service.TecnicoService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final TecnicoService tecnicoService;

    @GetMapping(value = "/tecnicos.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportTecnicosExcel() throws Exception {
        List<Tecnico> tecnicos = tecnicoService.listarTecnicos();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Técnicos");

            // Header style
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rownum = 0;
            var header = sheet.createRow(rownum++);
            String[] columns = new String[] {"ID", "Nombre completo", "Especialidad", "DNI", "Teléfono", "Ciudad", "Email", "Promedio"};
            for (int i = 0; i < columns.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Tecnico t : tecnicos) {
                var r = sheet.createRow(rownum++);
                int c = 0;
                r.createCell(c++).setCellValue(t.getIdTecnico() != null ? t.getIdTecnico() : 0);

                String nombre = (t.getNombres() != null ? t.getNombres() : "") + " " + (t.getApellidoPaterno() != null ? t.getApellidoPaterno() : "");
                if (t.getApellidoMaterno() != null && !t.getApellidoMaterno().isBlank()) nombre += " " + t.getApellidoMaterno();
                r.createCell(c++).setCellValue(nombre.trim());

                // Especialidad: primera categoria si existe
                String especialidad = "";
                try {
                    if (t.getCategorias() != null && !t.getCategorias().isEmpty() && t.getCategorias().get(0).getCategoria() != null) {
                        especialidad = t.getCategorias().get(0).getCategoria().getNombre();
                    }
                } catch (Exception ignored) {}
                r.createCell(c++).setCellValue(especialidad);

                r.createCell(c++).setCellValue(t.getDni() != null ? t.getDni() : "");
                r.createCell(c++).setCellValue(t.getTelefono() != null ? t.getTelefono() : "");

                String ciudad = "";
                try {
                    if (t.getDireccion() != null && t.getDireccion().getCiudad() != null) {
                        ciudad = t.getDireccion().getCiudad().getNombre();
                    }
                } catch (Exception ignored) {}
                r.createCell(c++).setCellValue(ciudad);

                String email = "";
                try { if (t.getUsuario() != null && t.getUsuario().getEmail() != null) email = t.getUsuario().getEmail(); } catch (Exception ignored) {}
                r.createCell(c++).setCellValue(email);

                Double prom = t.getPromedioCalificacion() != null ? t.getPromedioCalificacion() : 0.0;
                r.createCell(c++).setCellValue(prom);
            }

            // autosize columns
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                byte[] bytes = out.toByteArray();

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tecnicos.xlsx");
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentLength(bytes.length);

                return ResponseEntity.ok().headers(headers).body(bytes);
            }
        }
    }
}
