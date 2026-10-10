package com.hogarfix.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hogarfix.service.AuditoriaService;
import com.hogarfix.service.DisputaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DisputaController {

    private final DisputaService disputaService;
    private final AuditoriaService auditoriaService;

    /** Cliente o técnico reportan un problema. Vuelve a su pantalla con ?disputa=ok o ?disputa=error. */
    @PostMapping("/disputas/abrir")
    public String abrir(@RequestParam("servicioId") Long servicioId,
            @RequestParam(value = "motivo", required = false) String motivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "volver", required = false) String volver,
            Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        boolean tecnico = "tecnico".equals(volver);
        String base = tecnico ? "/tecnicos/panel" : "/clientes/perfil";
        try {
            var r = disputaService.abrir(servicioId, principal.getName(), motivo, descripcion);
            base = r.esTecnico() ? "/tecnicos/panel" : "/clientes/perfil";
            return "redirect:" + base + "?disputa=ok";
        } catch (IllegalStateException ex) {
            String msg = java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:" + base + "?disputa=error&msg=" + msg;
        }
    }

    // ---------------- Admin ----------------

    @GetMapping("/admin/disputas")
    public String listar(Model model) {
        var filas = disputaService.listarFilas();
        model.addAttribute("disputas", filas);
        model.addAttribute("abiertas", filas.stream().filter(f -> "ABIERTA".equals(f.getEstado())).count());
        return "admin/disputas";
    }

    @PostMapping("/admin/disputas/{id}/resolver")
    public String resolver(@PathVariable Long id, @RequestParam(value = "decision", required = false) String decision,
            @RequestParam(value = "nota", required = false) String nota, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/auth/login";
        try {
            disputaService.resolver(id, decision, nota, principal.getName());
            auditoriaService.registrar(principal.getName(), "Resolvió disputa", "Disputa #" + id + " — " + decision);
            ra.addFlashAttribute("mensajeExito", "Reporte #" + id + " resuelto. Se avisó a las dos partes.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/disputas";
    }
}
