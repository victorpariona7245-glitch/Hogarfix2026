package com.hogarfix.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.ClienteRepository;
import com.hogarfix.repository.TecnicoRepository;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.service.AuditoriaService;

import lombok.RequiredArgsConstructor;

/**
 * Suspender y reactivar clientes y técnicos sin borrarlos.
 * Una cuenta suspendida no puede iniciar sesión (y si tenía sesión abierta, se le cierra).
 * Sus datos, servicios y pagos se conservan.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminSuspensionController {

    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ---------------- Clientes ----------------

    @PostMapping("/clientes/{id}/suspender")
    public String suspenderCliente(@PathVariable Long id,
            @RequestParam(defaultValue = "") String motivo,
            Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        Optional<Cliente> cliente = clienteRepository.findById(id);
        if (cliente.isEmpty() || cliente.get().getUsuario() == null) {
            ra.addFlashAttribute("mensajeError", "El cliente no existe.");
        } else {
            cambiar(cliente.get().getUsuario(), true, motivo, principal, "cliente",
                    nombreCompleto(cliente.get().getNombres(), cliente.get().getApellidoPaterno()), ra);
        }
        return "redirect:/admin/clientes";
    }

    @PostMapping("/clientes/{id}/reactivar")
    public String reactivarCliente(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        Optional<Cliente> cliente = clienteRepository.findById(id);
        if (cliente.isEmpty() || cliente.get().getUsuario() == null) {
            ra.addFlashAttribute("mensajeError", "El cliente no existe.");
        } else {
            cambiar(cliente.get().getUsuario(), false, "", principal, "cliente",
                    nombreCompleto(cliente.get().getNombres(), cliente.get().getApellidoPaterno()), ra);
        }
        return "redirect:/admin/clientes";
    }

    // ---------------- Técnicos ----------------

    @PostMapping("/tecnicos/{id}/suspender")
    public String suspenderTecnico(@PathVariable Long id,
            @RequestParam(defaultValue = "") String motivo,
            Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        Optional<Tecnico> tecnico = tecnicoRepository.findById(id);
        if (tecnico.isEmpty() || tecnico.get().getUsuario() == null) {
            ra.addFlashAttribute("mensajeError", "El técnico no existe.");
        } else {
            cambiar(tecnico.get().getUsuario(), true, motivo, principal, "técnico",
                    nombreCompleto(tecnico.get().getNombres(), tecnico.get().getApellidoPaterno()), ra);
        }
        return "redirect:/admin/tecnicos";
    }

    @PostMapping("/tecnicos/{id}/reactivar")
    public String reactivarTecnico(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        Optional<Tecnico> tecnico = tecnicoRepository.findById(id);
        if (tecnico.isEmpty() || tecnico.get().getUsuario() == null) {
            ra.addFlashAttribute("mensajeError", "El técnico no existe.");
        } else {
            cambiar(tecnico.get().getUsuario(), false, "", principal, "técnico",
                    nombreCompleto(tecnico.get().getNombres(), tecnico.get().getApellidoPaterno()), ra);
        }
        return "redirect:/admin/tecnicos";
    }

    // ---------------- Común ----------------

    private void cambiar(Usuario usuario, boolean suspender, String motivo, Principal principal,
            String tipo, String nombre, RedirectAttributes ra) {
        String razon = motivo == null ? "" : motivo.trim();
        if (razon.length() > 255) {
            razon = razon.substring(0, 255);
        }
        usuario.setSuspendido(suspender);
        usuario.setMotivoSuspension(suspender && !razon.isEmpty() ? razon : null);
        usuarioRepository.save(usuario);

        if (suspender) {
            auditoriaService.registrar(principal.getName(), "Cuenta suspendida",
                    "Suspendió al " + tipo + " " + nombre + " (" + usuario.getEmail() + ")"
                            + (razon.isEmpty() ? "" : ". Motivo: " + razon));
            ra.addFlashAttribute("mensajeExito", "Cuenta de " + nombre + " suspendida.");
        } else {
            auditoriaService.registrar(principal.getName(), "Cuenta reactivada",
                    "Reactivó al " + tipo + " " + nombre + " (" + usuario.getEmail() + ")");
            ra.addFlashAttribute("mensajeExito", "Cuenta de " + nombre + " reactivada.");
        }
    }

    private static String nombreCompleto(String nombres, String apellido) {
        String n = nombres == null ? "" : nombres.trim();
        String a = apellido == null ? "" : apellido.trim();
        String completo = (n + " " + a).trim();
        return completo.isEmpty() ? "(sin nombre)" : completo;
    }
}
