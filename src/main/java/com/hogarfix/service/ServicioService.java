package com.hogarfix.service;

import com.hogarfix.model.Servicio;
import com.hogarfix.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

// Lógica de negocio para la gestión de solicitudes de servicios
@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public Servicio registrarServicio(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    public List<Servicio> listarServicios() {
        return servicioRepository.findAll();
    }

    public List<Servicio> listarPorCliente(com.hogarfix.model.Cliente cliente) {
        return servicioRepository.findByCliente(cliente);
    }

    public Optional<Servicio> buscarPorId(Long id) {
        return servicioRepository.findById(id);
    }

    public Optional<Servicio> marcarEnProgreso(Long id) {
        return servicioRepository.findById(id).map(s -> {
            s.setEstado("EN_PROCESO");
            return servicioRepository.save(s);
        });
    }

    public Optional<Servicio> marcarFinalizado(Long id) {
        return servicioRepository.findById(id).map(s -> {
            s.setEstado("FINALIZADO");
            s.setFechaFinalizacion(java.time.LocalDateTime.now());
            return servicioRepository.save(s);
        });
    }

    public Optional<Servicio> marcarCancelado(Long id) {
        return servicioRepository.findById(id).map(s -> {
            s.setEstado("CANCELADO");
            return servicioRepository.save(s);
        });
    }

    public void eliminarServicio(Long id) {
        servicioRepository.deleteById(id);
    }

    public List<Servicio> obtenerPorTecnico(Long idTecnico) {
        return servicioRepository.findByTecnico_IdTecnico(idTecnico);
    }
}
