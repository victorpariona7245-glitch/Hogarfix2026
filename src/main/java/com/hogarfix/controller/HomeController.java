package com.hogarfix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 

import com.hogarfix.service.CategoriaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoriaService categoriaService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "index";
    }
}
