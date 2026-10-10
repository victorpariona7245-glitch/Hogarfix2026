package com.hogarfix.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import com.hogarfix.repository.CalificacionRepository;
import com.hogarfix.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;

/** Datos para los gráficos del dashboard del administrador. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String[] MESES = { "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov",
            "dic" };

    private final ServicioRepository servicioRepository;
    private final CalificacionRepository calificacionRepository;
    private final TecnicoService tecnicoService;

    /** Un punto de un gráfico: etiqueta, valor y un detalle opcional para el tooltip. */
    public record Punto(String etiqueta, double valor, String detalle) {}

    public record Datos(List<Punto> solicitudesPorMes, List<Punto> especialidades, List<Punto> tecnicos,
            long solicitudesMes, long solicitudesTotal, long finalizadas) {}

    @Transactional(readOnly = true)
    public Datos calcular() {
        List<Servicio> servicios = servicioRepository.findAll();

        // Últimos 12 meses (incluye el actual), aunque estén en cero
        YearMonth actual = YearMonth.now();
        Map<YearMonth, Long> porMes = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) porMes.put(actual.minusMonths(i), 0L);

        Map<String, Long> porEspecialidad = new HashMap<>();
        long finalizadas = 0;
        for (Servicio s : servicios) {
            LocalDateTime f = s.getFechaSolicitud();
            if (f != null) porMes.computeIfPresent(YearMonth.from(f), (k, v) -> v + 1);
            String cat = s.getCategoria() != null ? s.getCategoria().getNombre() : "Sin categoría";
            porEspecialidad.merge(cat, 1L, Long::sum);
            if ("FINALIZADO".equals(s.getEstado())) finalizadas++;
        }

        List<Punto> meses = new ArrayList<>();
        porMes.forEach((ym, n) -> meses.add(new Punto(MESES[ym.getMonthValue() - 1] + " " + (ym.getYear() % 100), n, null)));

        List<Punto> especialidades = porEspecialidad.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .map(e -> new Punto(e.getKey(), e.getValue(), null))
                .toList();

        Map<Long, Long> resenas = new HashMap<>();
        for (Object[] fila : calificacionRepository.contarPorTecnico()) {
            resenas.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        List<Punto> tecnicos = new ArrayList<>();
        for (Tecnico t : tecnicoService.listarTecnicosAprobados()) {
            long n = resenas.getOrDefault(t.getIdTecnico(), 0L);
            double prom = t.getPromedioCalificacion() != null ? t.getPromedioCalificacion() : 0.0;
            if (n == 0 || prom <= 0) continue;
            tecnicos.add(new Punto(t.getNombres() + " " + (t.getApellidoPaterno() != null ? t.getApellidoPaterno() : ""),
                    Math.round(prom * 10) / 10.0, n + (n == 1 ? " reseña" : " reseñas")));
        }
        tecnicos.sort(Comparator.comparingDouble(Punto::valor).reversed());
        if (tecnicos.size() > 5) tecnicos = new ArrayList<>(tecnicos.subList(0, 5));

        long mesActual = porMes.getOrDefault(actual, 0L);
        return new Datos(meses, especialidades, tecnicos, mesActual, servicios.size(), finalizadas);
    }
}
