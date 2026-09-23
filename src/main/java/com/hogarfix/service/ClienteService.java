package com.hogarfix.service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.hogarfix.model.Cliente;
import com.hogarfix.model.Direccion;
import com.hogarfix.model.Rol;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.ClienteRepository;
import com.hogarfix.repository.RolRepository;
import com.hogarfix.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final Logger logger = LoggerFactory.getLogger(ClienteService.class);
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public Cliente registrarCliente(Cliente cliente) {
        try {
            if (cliente.getUsuario() == null || cliente.getUsuario().getEmail() == null) {
                logger.error("Usuario o email inválido al intentar registrar cliente");
                throw new RuntimeException("Usuario o email inválido");
            }

            String email = cliente.getUsuario().getEmail();
            String dni = cliente.getDni();

            // Validar email duplicado
            if (clienteRepository.existsByUsuario_Email(email)) {
                logger.warn("Intento de registro con email duplicado: {}", email);
                throw new RuntimeException("El correo ya está registrado");
            }

            // Validar DNI duplicado
            if (clienteRepository.existsByDni(dni)) {
                logger.warn("Intento de registro con DNI duplicado: {}", dni);
                throw new RuntimeException("El DNI ya está registrado");
            }

            // Rol CLIENTE por defecto
            Rol rol = rolRepository.findByNombre("CLIENTE")
                    .orElseGet(() -> rolRepository.save(
                            Rol.builder()
                                    .nombre("CLIENTE")
                                    .descripcion("Rol por defecto para clientes")
                                    .build()));

            cliente.getUsuario().setRol(rol);
            cliente.getUsuario().setPassword(passwordEncoder.encode(cliente.getUsuario().getPassword()));

            // Guardar usuario primero
            var usuarioGuardado = usuarioRepository.save(cliente.getUsuario());
            cliente.setUsuario(usuarioGuardado);

            Cliente clienteGuardado = clienteRepository.save(cliente);

            logger.info("Cliente registrado exitosamente: email={}, dni={}, id={}",
                    email, dni, clienteGuardado.getIdCliente());

            return clienteGuardado;

        } catch (Exception e) {
            logger.error("Error al registrar cliente: email={}, causa={}",
                    cliente.getUsuario() != null ? cliente.getUsuario().getEmail() : "desconocido",
                    e.getMessage(), e);
            throw e;
        }
    }

    // Autenticación simple
    public Optional<Cliente> autenticar(String email, String password) {
        try {
            var resultado = clienteRepository.findByUsuario_Email(email)
                    .filter(c -> passwordEncoder.matches(password, c.getUsuario().getPassword()));

            if (resultado.isPresent()) {
                logger.info("Autenticación exitosa de cliente: {}", email);
            } else {
                logger.warn("Fallo de autenticación para cliente: {}", email);
            }
            return resultado;
        } catch (Exception e) {
            logger.error("Error durante autenticación de cliente: {}", email, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        try {
            var clientes = clienteRepository.findAll();
            // Inicializar asociaciones perezosas dentro de la transacción del servicio
            for (Cliente c : clientes) {
                if (c.getUsuario() != null) {
                    // tocar una propiedad para forzar carga
                    c.getUsuario().getEmail();
                }
                if (c.getDireccion() != null) {
                    c.getDireccion().getCiudad();
                }
            }
            logger.debug("Listando {} clientes de BD", clientes.size());
            return clientes;
        } catch (Exception e) {
            logger.error("Error al listar clientes de BD", e);
            throw e;
        }
    }

    public Optional<Cliente> buscarPorId(Long id) {
        try {
            return clienteRepository.findById(id);
        } catch (Exception e) {
            logger.error("Error al buscar cliente por id en BD: id={}", id, e);
            throw e;
        }
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        try {
            return clienteRepository.findByUsuario_Email(email);
        } catch (Exception e) {
            logger.error("Error al buscar cliente por email en BD: email={}", email, e);
            throw e;
        }
    }

    public Optional<Cliente> buscarPorUsername(String username) {
        try {
            return clienteRepository.findByUsuario_Username(username);
        } catch (Exception e) {
            logger.error("Error al buscar cliente por username en BD: username={}", username, e);
            throw e;
        }
    }

    public Cliente obtenerPorEmail(String email) {
        try {
            return clienteRepository.findByUsuario_Email(email)
                    .orElseThrow(() -> new RuntimeException("No se encontró el cliente con email: " + email));
        } catch (Exception e) {
            logger.error("Error al obtener cliente por email: {}", email, e);
            throw e;
        }
    }

    public void eliminarCliente(Long id) {
        try {
            clienteRepository.deleteById(id);
            logger.info("Cliente eliminado de BD: id={}", id);
        } catch (Exception e) {
            logger.error("Error al eliminar cliente de BD: id={}", id, e);
            throw e;
        }
    }

    @Transactional
    public void actualizarCliente(Cliente cliente) {

        Cliente clienteBD = clienteRepository.findById(cliente.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // --- Usuario ---
        Usuario usuarioBD = clienteBD.getUsuario();
        Usuario usuarioNuevo = cliente.getUsuario();

        usuarioBD.setEmail(usuarioNuevo.getEmail());
        usuarioBD.setUsername(usuarioNuevo.getEmail());
        // Mantener password
        usuarioBD.setPassword(usuarioBD.getPassword());

        // --- Cliente ---
        clienteBD.setNombres(cliente.getNombres());
        clienteBD.setApellidoPaterno(cliente.getApellidoPaterno());
        clienteBD.setApellidoMaterno(cliente.getApellidoMaterno());
        clienteBD.setTelefono(cliente.getTelefono());

        // --- Dirección (forma correcta REAL) ---
        Direccion dirBD = clienteBD.getDireccion();
        Direccion dirNueva = cliente.getDireccion();

        dirBD.setCalle(dirNueva.getCalle());
        dirBD.setNumero(dirNueva.getNumero());
        dirBD.setReferencia(dirNueva.getReferencia());
        dirBD.setCiudad(dirNueva.getCiudad());

        // El save no es obligatorio en transacción, pero lo dejamos
        clienteRepository.save(clienteBD);

        logger.info("Cliente actualizado exitosamente: id={}, email={}",
                clienteBD.getIdCliente(), usuarioBD.getEmail());
    }
}