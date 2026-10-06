package com.hogarfix.service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.TecnicoCategoria;
import com.hogarfix.model.Usuario;
import com.hogarfix.model.Categoria;
import com.hogarfix.model.Rol;
import com.hogarfix.repository.TecnicoRepository;
import com.hogarfix.repository.CategoriaRepository;
import com.hogarfix.repository.RolRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Habilita la cuenta de un técnico (permite que pueda iniciar sesión),
     * normalmente después de que el administrador revisó su certificado.
     */
    public Tecnico habilitarCuenta(Long idTecnico) {
        Tecnico tecnico = tecnicoRepository.findById(idTecnico)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado: " + idTecnico));
        if (tecnico.getUsuario() == null) {
            throw new RuntimeException("El técnico no tiene una cuenta de usuario asociada");
        }
        tecnico.getUsuario().setIsActivo(true);
        usuarioRepository.save(tecnico.getUsuario());
        log.info("Cuenta de técnico habilitada: id={}, email={}", idTecnico, tecnico.getUsuario().getEmail());
        return tecnico;
    }

    /**
     * Deshabilita la cuenta de un técnico (por ejemplo, si se detecta un problema
     * con su certificado o comportamiento).
     */
    public Tecnico deshabilitarCuenta(Long idTecnico) {
        Tecnico tecnico = tecnicoRepository.findById(idTecnico)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado: " + idTecnico));
        if (tecnico.getUsuario() == null) {
            throw new RuntimeException("El técnico no tiene una cuenta de usuario asociada");
        }
        tecnico.getUsuario().setIsActivo(false);
        usuarioRepository.save(tecnico.getUsuario());
        log.info("Cuenta de técnico deshabilitada: id={}, email={}", idTecnico, tecnico.getUsuario().getEmail());
        return tecnico;
    }

    public Tecnico registrarTecnico(Tecnico tecnico) {
        try {
            if (tecnico.getUsuario() == null || tecnico.getUsuario().getEmail() == null) {
                log.error("Usuario o email inválido al intentar registrar técnico");
                throw new RuntimeException("Usuario o email inválido");
            }

            String email = tecnico.getUsuario().getEmail();
            String dni = tecnico.getDni();

            // Validar email duplicado
            if (tecnicoRepository.existsByUsuario_Email(email)) {
                log.warn("Intento de registro con email duplicado: {}", email);
                throw new RuntimeException("El correo ya está registrado");
            }

            // Validar DNI duplicado
            if (tecnicoRepository.existsByDni(dni)) {
                log.warn("Intento de registro con DNI duplicado: {}", dni);
                throw new RuntimeException("El DNI ya está registrado");
            }

            // Rol por defecto
            Rol rol = rolRepository.findByNombre("TECNICO")
                    .orElseGet(() -> rolRepository.save(
                            Rol.builder()
                                    .nombre("TECNICO")
                                    .descripcion("Rol por defecto para técnicos")
                                    .build()));

            tecnico.getUsuario().setRol(rol);
            tecnico.getUsuario().setPassword(passwordEncoder.encode(tecnico.getUsuario().getPassword()));
            // La cuenta queda pendiente de aprobación hasta que el administrador
            // revise su certificado profesional y la habilite
            tecnico.getUsuario().setIsActivo(false);

            // Guardar usuario primero
            var usuarioGuardado = usuarioRepository.save(tecnico.getUsuario());
            tecnico.setUsuario(usuarioGuardado);

            Tecnico saved = tecnicoRepository.save(tecnico);

            log.info("Técnico registrado exitosamente: email={}, dni={}, id={}",
                    email, dni, saved.getIdTecnico());

            return saved;

        } catch (Exception e) {
            log.error("Error al registrar técnico: email={}, causa={}",
                    tecnico.getUsuario() != null ? tecnico.getUsuario().getEmail() : "desconocido",
                    e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Tecnico> listarTecnicos() {
        try {
            var tecnicos = tecnicoRepository.findAll();
            // Inicializar asociaciones perezosas (usuario, categorias, direccion)
            for (Tecnico t : tecnicos) {
                if (t.getUsuario() != null) {
                    t.getUsuario().getEmail();
                }
                if (t.getCategorias() != null) {
                    t.getCategorias().size();
                }
                if (t.getDireccion() != null) {
                    t.getDireccion().getCiudad();
                }
            }
            log.debug("Listando {} técnicos de BD", tecnicos.size());
            return tecnicos;
        } catch (Exception e) {
            log.error("Error al listar técnicos de BD", e);
            throw e;
        }
    }

    /**
     * Permite al propio técnico marcarse como disponible o no disponible.
     * A diferencia de habilitarCuenta/deshabilitarCuenta (que controla el admin
     * para aprobar la cuenta), este campo lo controla el técnico libremente.
     */
    public Tecnico cambiarDisponibilidad(Long idTecnico, boolean disponible) {
        Tecnico tecnico = tecnicoRepository.findById(idTecnico)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado: " + idTecnico));
        tecnico.setDisponible(disponible);
        Tecnico guardado = tecnicoRepository.save(tecnico);
        log.info("Disponibilidad de técnico actualizada: id={}, disponible={}", idTecnico, disponible);
        return guardado;
    }

    public Optional<Tecnico> buscarPorId(Long id) {
        try {
            return tecnicoRepository.findById(id);
        } catch (Exception e) {
            log.error("Error al buscar técnico por id en BD: id={}", id, e);
            throw e;
        }
    }

    public List<Tecnico> buscarPorCategoria(String categoria) {
        try {
            log.debug("Buscando técnicos por categoría: {}", categoria);
            return tecnicoRepository.findTecnicosByCategoriaNombre(categoria);
        } catch (Exception e) {
            log.error("Error al buscar técnicos por categoría: {}", categoria, e);
            throw e;
        }
    }

    public void actualizarCategorias(Tecnico tecnico, List<Long> idsCategoriasSeleccionadas) {

        // 1. Limpiar categorías anteriores
        tecnico.getCategorias().clear();

        // 2. Reconstruir las relaciones
        for (Long idCat : idsCategoriasSeleccionadas) {
            Categoria categoria = categoriaRepository.findById(idCat)
                    .orElseThrow(() -> new RuntimeException("Categoria no existe: " + idCat));

            TecnicoCategoria tc = TecnicoCategoria.builder()
                    .tecnico(tecnico)
                    .categoria(categoria)
                    .build();

            tecnico.getCategorias().add(tc);
        }

        // 3. Guardar el técnico (cascade ALL hará el resto)
        tecnicoRepository.save(tecnico);
    }

    public Optional<Tecnico> autenticar(String email, String password) {
        try {
            var resultado = tecnicoRepository.findByUsuario_Email(email)
                    .filter(t -> passwordEncoder.matches(password, t.getUsuario().getPassword()));

            if (resultado.isPresent()) {
                log.info("Autenticación exitosa de técnico: {}", email);
            } else {
                log.warn("Fallo de autenticación para técnico: {}", email);
            }
            return resultado;
        } catch (Exception e) {
            log.error("Error durante autenticación de técnico: {}", email, e);
            throw e;
        }
    }

    public Tecnico obtenerPorEmail(String email) {
        try {
            return tecnicoRepository.findByUsuario_Email(email)
                    .orElseThrow(() -> new RuntimeException("No se encontró el técnico con email: " + email));
        } catch (Exception e) {
            log.error("Error al obtener técnico por email: {}", email, e);
            throw e;
        }
    }

    public Tecnico obtenerPorEmailODni(String emailAuth) {
        // 1. Intentar por email
        Optional<Tecnico> porEmail = tecnicoRepository.findByUsuario_Email(emailAuth);
        if (porEmail.isPresent()) {
            return porEmail.get();
        }

        // 2. Si no está por email, buscar por dni (ya que es único)
        Optional<Usuario> usuario = usuarioRepository.findByEmail(emailAuth);
        if (usuario.isPresent()) {
            // emailAuth corresponde a un usuario pero ya fue cambiado
            // entonces obtenemos el técnico usando su DNI original
            return tecnicoRepository.findByDni(usuario.get().getTecnico().getDni())
                    .orElseThrow(() -> new RuntimeException("No se encontró técnico por DNI del usuario"));
        }

        throw new RuntimeException("No se encontró técnico por email o DNI: " + emailAuth);
    }

}