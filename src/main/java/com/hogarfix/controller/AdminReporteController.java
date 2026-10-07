package com.hogarfix.controller;

import java.math.BigDecimal;
import java.security.Principal;
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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hogarfix.model.Pago;
import com.hogarfix.model.Servicio;
import com.hogarfix.repository.PagoRepository;
import com.hogarfix.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reporte mensual: servicios solicitados y atendidos, técnicos que atendieron
 * y monto cobrado, todo desglosado por especialidad.
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

    @GetMapping
    public String ver(@RequestParam(required = false) String mes, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        YearMonth periodo;
        try {
            periodo = (mes == null || mes.isBlank()) ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException ex) {
            periodo = YearMonth.now();
        }
        LocalDateTime desde = periodo.atDay(1).atStartOfDay();
        LocalDateTime hasta = periodo.plusMonths(1).atDay(1).atStartOfDay();

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
            long[] c = conteos.computeIfAbsent(esp, k -> new long[3]);
            c[0]++;
            String estado = s.getEstado() == null ? "" : s.getEstado();
            if (estado.equals("EN_PROCESO") || estado.equals("FINALIZADO")) {
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
            montos.merge(esp, p.getMonto(), BigDecimal::add);
            montoTotal = montoTotal.add(p.getMonto());
        }

        Set<String> especialidades = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        especialidades.addAll(conteos.keySet());
        especialidades.addAll(montos.keySet());

        List<Fila> filas = new ArrayList<>();
        long totSolicitados = 0, totAtendidos = 0, totFinalizados = 0;
        for (String esp : especialidades) {
            long[] c = conteos.getOrDefault(esp, new long[3]);
            filas.add(new Fila(esp, c[0], c[1], c[2],
                    tecnicosPorEsp.getOrDefault(esp, Set.of()).size(),
                    montos.getOrDefault(esp, BigDecimal.ZERO)));
            totSolicitados += c[0];
            totAtendidos += c[1];
            totFinalizados += c[2];
        }

        String nombreMes = periodo.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        model.addAttribute("mes", periodo.toString());
        model.addAttribute("mesTexto", nombreMes + " " + periodo.getYear());
        model.addAttribute("filas", filas);
        model.addAttribute("totSolicitados", totSolicitados);
        model.addAttribute("totAtendidos", totAtendidos);
        model.addAttribute("totFinalizados", totFinalizados);
        model.addAttribute("totTecnicos", tecnicosTotal.size());
        model.addAttribute("montoTotal", montoTotal);
        return "admin/reportes";
    }
}
