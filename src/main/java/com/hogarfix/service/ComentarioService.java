package com.hogarfix.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.hogarfix.model.Comentario;
import com.hogarfix.repository.ComentarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;

    public Comentario registrarComentario(Comentario comentario) {
        return comentarioRepository.save(comentario);
    }

    public List<Comentario> listarComentarios() {
        return comentarioRepository.findAll();
    }
}
