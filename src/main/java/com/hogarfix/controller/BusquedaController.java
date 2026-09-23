package com.hogarfix.controller;

import com.hogarfix.model.Tecnico;
import com.hogarfix.model.TecnicoCategoria;
import com.hogarfix.service.CategoriaService;
import com.hogarfix.service.CiudadService;
import com.hogarfix.service.TecnicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class BusquedaController {

    private final TecnicoService tecnicoService;
    private final CiudadService ciudadService;
    private final CategoriaService categoriaService;

    @GetMapping("/buscartecnicos")
    public String buscarTecnicos(
            @RequestParam(value = "ciudadId", required = false) Long ciudadId,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "minCalificacion", required = false) Integer minCalificacion,
            Model model) {

        List<Tecnico> tecnicos = tecnicoService.listarTecnicos();

        double minCal = minCalificacion != null ? minCalificacion : 0;

        List<Tecnico> filtrados = tecnicos.stream()
                .filter(t -> {
                    if (ciudadId == null) return true;
                    return t.getDireccion() != null && t.getDireccion().getCiudad() != null && Objects.equals(t.getDireccion().getCiudad().getIdCiudad(), ciudadId);
                })
                .filter(t -> {
                    if (categoriaId == null) return true;
                    List<TecnicoCategoria> cats = t.getCategorias();
                    if (cats == null || cats.isEmpty()) return false;
                    return cats.stream().anyMatch(tc -> tc.getCategoria() != null && Objects.equals(tc.getCategoria().getIdCategoria(), categoriaId));
                })
                .filter(t -> {
                    double prom = t.getPromedioCalificacion() != null ? t.getPromedioCalificacion() : 0.0;
                    return prom >= minCal;
                })
                .collect(Collectors.toList());

        model.addAttribute("tecnicos", filtrados);
        model.addAttribute("ciudades", ciudadService.listarCiudades());
        model.addAttribute("categorias", categoriaService.listarCategorias());

        return "buscartecnicos";
    }
}
