package com.hogarfix.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hogarfix.repository.RegistroActividadRepository;

import lombok.RequiredArgsConstructor;

/** Registro de actividad: quién creó, eliminó o cambió qué, y cuándo. Solo lectura. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/actividad")
public class AdminActividadController {

    private static final int POR_PAGINA = 20;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RegistroActividadRepository repository;

    public record Fila(String fecha, String administrador, String accion, String detalle) {
    }

    @GetMapping
    public String ver(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        String busqueda = q == null ? "" : q.trim();
        if (busqueda.length() > 100) {
            busqueda = busqueda.substring(0, 100);
        }
        Page<com.hogarfix.model.RegistroActividad> pagina = repository.buscar(busqueda,
                PageRequest.of(Math.max(page, 0), POR_PAGINA));

        List<Fila> filas = pagina.getContent().stream()
                .map(r -> new Fila(r.getFecha().format(FECHA), r.getAdministrador(), r.getAccion(), r.getDetalle()))
                .toList();

        model.addAttribute("filas", filas);
        model.addAttribute("q", busqueda);
        model.addAttribute("pagina", pagina.getNumber());
        model.addAttribute("totalPaginas", Math.max(1, pagina.getTotalPages()));
        model.addAttribute("total", pagina.getTotalElements());
        return "admin/actividad";
    }
}
