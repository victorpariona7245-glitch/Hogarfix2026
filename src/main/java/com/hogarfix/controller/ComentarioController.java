package com.hogarfix.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hogarfix.model.Comentario;
import com.hogarfix.service.ComentarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    @GetMapping
    public ResponseEntity<List<Comentario>> listar() {
        return ResponseEntity.ok(comentarioService.listarComentarios());
    }

    @PostMapping
    public ResponseEntity<Comentario> registrar(@RequestBody Comentario comentario) {
        return ResponseEntity.ok(comentarioService.registrarComentario(comentario));
    }
}
