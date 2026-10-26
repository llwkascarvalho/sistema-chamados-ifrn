package br.edu.ifrn.chamados.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifrn.chamados.dto.chamado.ChamadoResumoResponse;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioResponse;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.service.ChamadoService;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ChamadoService chamadoService;

    @GetMapping
    public Page<UsuarioResponse> listar(@RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoUsuario tipo,
            @RequestParam(required = false) Long setorId, Pageable pageable) {
        return usuarioService.listar(nome, tipo, setorId, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return usuarioService.buscar(id);
    }

    @GetMapping("/{id}/chamados")
    public Page<ChamadoResumoResponse> listarChamados(@PathVariable Long id,
            @RequestParam(required = false) StatusChamado status, Pageable pageable) {
        return chamadoService.listarPorUsuario(id, status, pageable);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.criar(request);
        return ResponseEntity.created(URI.create("/usuarios/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.atualizar(id, request);
    }
}
