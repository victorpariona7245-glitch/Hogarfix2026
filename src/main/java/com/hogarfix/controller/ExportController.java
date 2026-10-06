package com.hogarfix.controller;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Pago;
import com.hogarfix.model.Tecnico;
import com.hogarfix.service.ClienteService;
import com.hogarfix.service.PagoService;
import com.hogarfix.service.TecnicoService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportación de reportes (CSV / Excel) para el panel de administración.
 * Todas las rutas quedan bajo /admin/** y por tanto ya están protegidas por
 * SecurityConfig (solo ROLE_ADMIN puede acceder).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/exportar")
public class ExportController {

    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;
    private final PagoService pagoService;

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NOMBRE_ARCHIVO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    // ==================== CLIENTES ====================

    @GetMapping("/clientes.csv")
    public ResponseEntity<byte[]> clientesCsv() {
        List<Cliente> clientes = clienteService.listarClientes();
        StringBuilder sb = new StringBuilder();
        agregarFilaCsv(sb, "ID", "Nombres", "Apellido Paterno", "Apellido Materno", "DNI", "Telefono", "Correo", "Ciudad", "Fecha Registro");
        for (Cliente c : clientes) {
            agregarFilaCsv(sb,
                    idComoTexto(c.getIdCliente()),
                    c.getNombres(),
                    c.getApellidoPaterno(),
                    c.getApellidoMaterno(),
                    c.getDni(),
                    c.getTelefono(),
                    correoDe(c.getUsuario()),
                    ciudadDe(c.getDireccion()),
                    fechaComoTexto(c.getCreatedAt()));
        }
        return respuestaCsv(sb.toString(), "clientes");
    }

    @GetMapping("/clientes.xlsx")
    public ResponseEntity<byte[]> clientesExcel() throws IOException {
        String[] encabezados = {"ID", "Nombres", "Apellido Paterno", "Apellido Materno", "DNI", "Teléfono", "Correo", "Ciudad", "Fecha Registro"};
        List<Cliente> clientes = clienteService.listarClientes();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Clientes");
            escribirEncabezado(wb, sheet, encabezados);
            int fila = 1;
            for (Cliente c : clientes) {
                Row r = sheet.createRow(fila++);
                int col = 0;
                r.createCell(col++).setCellValue(idComoNumero(c.getIdCliente()));
                r.createCell(col++).setCellValue(valor(c.getNombres()));
                r.createCell(col++).setCellValue(valor(c.getApellidoPaterno()));
                r.createCell(col++).setCellValue(valor(c.getApellidoMaterno()));
                r.createCell(col++).setCellValue(valor(c.getDni()));
                r.createCell(col++).setCellValue(valor(c.getTelefono()));
                r.createCell(col++).setCellValue(correoDe(c.getUsuario()));
                r.createCell(col++).setCellValue(ciudadDe(c.getDireccion()));
                r.createCell(col).setCellValue(fechaComoTexto(c.getCreatedAt()));
            }
            autoajustarColumnas(sheet, encabezados.length);
            return respuestaExcel(wb, "clientes");
        }
    }

    // ==================== TECNICOS ====================

    @GetMapping("/tecnicos.csv")
    public ResponseEntity<byte[]> tecnicosCsv() {
        List<Tecnico> tecnicos = tecnicoService.listarTecnicos();
        StringBuilder sb = new StringBuilder();
        agregarFilaCsv(sb, "ID", "Nombres", "Apellido Paterno", "Apellido Materno", "DNI", "Telefono", "Correo", "Ciudad", "Especialidades", "Calificacion", "Disponible");
        for (Tecnico t : tecnicos) {
            agregarFilaCsv(sb,
                    idComoTexto(t.getIdTecnico()),
                    t.getNombres(),
                    t.getApellidoPaterno(),
                    t.getApellidoMaterno(),
                    t.getDni(),
                    t.getTelefono(),
                    correoDe(t.getUsuario()),
                    ciudadDe(t.getDireccion()),
                    especialidadesDe(t),
                    calificacionComoTexto(t.getPromedioCalificacion()),
                    Boolean.TRUE.equals(t.getDisponible()) ? "Si" : "No");
        }
        return respuestaCsv(sb.toString(), "tecnicos");
    }

    @GetMapping("/tecnicos.xlsx")
    public ResponseEntity<byte[]> tecnicosExcel() throws IOException {
        String[] encabezados = {"ID", "Nombres", "Apellido Paterno", "Apellido Materno", "DNI", "Teléfono", "Correo", "Ciudad", "Especialidades", "Calificación", "Disponible"};
        List<Tecnico> tecnicos = tecnicoService.listarTecnicos();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Técnicos");
            escribirEncabezado(wb, sheet, encabezados);
            int fila = 1;
            for (Tecnico t : tecnicos) {
                Row r = sheet.createRow(fila++);
                int col = 0;
                r.createCell(col++).setCellValue(idComoNumero(t.getIdTecnico()));
                r.createCell(col++).setCellValue(valor(t.getNombres()));
                r.createCell(col++).setCellValue(valor(t.getApellidoPaterno()));
                r.createCell(col++).setCellValue(valor(t.getApellidoMaterno()));
                r.createCell(col++).setCellValue(valor(t.getDni()));
                r.createCell(col++).setCellValue(valor(t.getTelefono()));
                r.createCell(col++).setCellValue(correoDe(t.getUsuario()));
                r.createCell(col++).setCellValue(ciudadDe(t.getDireccion()));
                r.createCell(col++).setCellValue(especialidadesDe(t));
                r.createCell(col++).setCellValue(t.getPromedioCalificacion() != null ? t.getPromedioCalificacion() : 0.0);
                r.createCell(col).setCellValue(Boolean.TRUE.equals(t.getDisponible()) ? "Sí" : "No");
            }
            autoajustarColumnas(sheet, encabezados.length);
            return respuestaExcel(wb, "tecnicos");
        }
    }

    // ==================== PAGOS ====================

    @GetMapping("/pagos.csv")
    public ResponseEntity<byte[]> pagosCsv() {
        List<Pago> pagos = pagoService.listarPagosParaExportar();
        StringBuilder sb = new StringBuilder();
        agregarFilaCsv(sb, "ID", "Cliente", "Tecnico", "Servicio", "Monto (S/)", "Metodo de pago", "Estado", "Fecha de pago", "Referencia");
        for (Pago p : pagos) {
            agregarFilaCsv(sb,
                    idComoTexto(p.getIdPago()),
                    nombreClienteDe(p),
                    nombreTecnicoDe(p),
                    p.getServicio() != null ? valor(p.getServicio().getDescripcion()) : "",
                    p.getMonto() != null ? p.getMonto().toPlainString() : "0",
                    p.getMetodoPago(),
                    p.getEstado(),
                    p.getFechaPago() != null ? p.getFechaPago().format(FECHA_HORA) : "",
                    p.getReferenciaPago());
        }
        return respuestaCsv(sb.toString(), "pagos");
    }

    @GetMapping("/pagos.xlsx")
    public ResponseEntity<byte[]> pagosExcel() throws IOException {
        String[] encabezados = {"ID", "Cliente", "Técnico", "Servicio", "Monto (S/)", "Método de pago", "Estado", "Fecha de pago", "Referencia"};
        List<Pago> pagos = pagoService.listarPagosParaExportar();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Pagos");
            escribirEncabezado(wb, sheet, encabezados);
            int fila = 1;
            for (Pago p : pagos) {
                Row r = sheet.createRow(fila++);
                int col = 0;
                r.createCell(col++).setCellValue(idComoNumero(p.getIdPago()));
                r.createCell(col++).setCellValue(nombreClienteDe(p));
                r.createCell(col++).setCellValue(nombreTecnicoDe(p));
                r.createCell(col++).setCellValue(p.getServicio() != null ? valor(p.getServicio().getDescripcion()) : "");
                r.createCell(col++).setCellValue(p.getMonto() != null ? p.getMonto().doubleValue() : 0.0);
                r.createCell(col++).setCellValue(valor(p.getMetodoPago()));
                r.createCell(col++).setCellValue(valor(p.getEstado()));
                r.createCell(col++).setCellValue(p.getFechaPago() != null ? p.getFechaPago().format(FECHA_HORA) : "");
                r.createCell(col).setCellValue(valor(p.getReferenciaPago()));
            }
            autoajustarColumnas(sheet, encabezados.length);
            return respuestaExcel(wb, "pagos");
        }
    }

    // ==================== HELPERS DE DATOS ====================

    private String nombreClienteDe(Pago p) {
        if (p.getCliente() == null) return "";
        return (valor(p.getCliente().getNombres()) + " " + valor(p.getCliente().getApellidoPaterno())).trim();
    }

    private String nombreTecnicoDe(Pago p) {
        if (p.getServicio() == null || p.getServicio().getTecnico() == null) return "Sin asignar";
        var t = p.getServicio().getTecnico();
        return (valor(t.getNombres()) + " " + valor(t.getApellidoPaterno())).trim();
    }

    private String especialidadesDe(Tecnico t) {
        if (t.getCategorias() == null || t.getCategorias().isEmpty()) return "Sin especificar";
        StringBuilder sb = new StringBuilder();
        for (var tc : t.getCategorias()) {
            if (tc.getCategoria() != null) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(tc.getCategoria().getNombre());
            }
        }
        return sb.length() > 0 ? sb.toString() : "Sin especificar";
    }

    private String correoDe(com.hogarfix.model.Usuario usuario) {
        return usuario != null ? valor(usuario.getEmail()) : "";
    }

    private String ciudadDe(com.hogarfix.model.Direccion direccion) {
        return (direccion != null && direccion.getCiudad() != null) ? valor(direccion.getCiudad().getNombre()) : "";
    }

    private String fechaComoTexto(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FECHA_HORA) : "";
    }

    private String calificacionComoTexto(Double calificacion) {
        return String.format("%.2f", calificacion != null ? calificacion : 0.0);
    }

    private String idComoTexto(Long id) {
        return id != null ? String.valueOf(id) : "";
    }

    private double idComoNumero(Long id) {
        return id != null ? id : 0;
    }

    private String valor(String s) {
        return s == null ? "" : s;
    }

    // ==================== HELPERS DE FORMATO/ARCHIVO ====================

    private void agregarFilaCsv(StringBuilder sb, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escaparCsv(valores[i]));
        }
        sb.append("\r\n");
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        boolean necesitaComillas = valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r");
        String limpio = valor.replace("\"", "\"\"");
        return necesitaComillas ? "\"" + limpio + "\"" : limpio;
    }

    private ResponseEntity<byte[]> respuestaCsv(String contenido, String nombreBase) {
        // BOM UTF-8 para que Excel detecte tildes/ñ correctamente al abrir el CSV
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] cuerpo = contenido.getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[bom.length + cuerpo.length];
        System.arraycopy(bom, 0, resultado, 0, bom.length);
        System.arraycopy(cuerpo, 0, resultado, bom.length, cuerpo.length);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreArchivo(nombreBase, "csv"), StandardCharsets.UTF_8)
                                .build().toString())
                .body(resultado);
    }

    private ResponseEntity<byte[]> respuestaExcel(Workbook wb, String nombreBase) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreArchivo(nombreBase, "xlsx"), StandardCharsets.UTF_8)
                                .build().toString())
                .body(out.toByteArray());
    }

    private String nombreArchivo(String base, String extension) {
        return base + "_" + LocalDateTime.now().format(NOMBRE_ARCHIVO_FMT) + "." + extension;
    }

    private void escribirEncabezado(Workbook wb, Sheet sheet, String[] encabezados) {
        Font fuenteNegrita = wb.createFont();
        fuenteNegrita.setBold(true);
        fuenteNegrita.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estilo = wb.createCellStyle();
        estilo.setFont(fuenteNegrita);
        estilo.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row fila = sheet.createRow(0);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estilo);
        }
        sheet.createFreezePane(0, 1);
    }

    private void autoajustarColumnas(Sheet sheet, int numColumnas) {
        for (int i = 0; i < numColumnas; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
