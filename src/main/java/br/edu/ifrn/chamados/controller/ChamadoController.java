package br.edu.ifrn.chamados.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoFiltro;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResumoResponse;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResponse;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoResponse;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;

    @GetMapping
    public Page<ChamadoResumoResponse> listar(@Valid @ModelAttribute ChamadoFiltro filtro, Pageable pageable) {
        return chamadoService.listar(filtro, pageable);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse response = chamadoService.abrir(request);
        return ResponseEntity.created(URI.create("/chamados/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscar(@PathVariable Long id) {
        return chamadoService.buscar(id);
    }

    @PatchMapping("/{id}/assumir")
    public ChamadoResponse assumir(@PathVariable Long id, @Valid @RequestBody AssumirChamadoRequest request) {
        return chamadoService.assumir(id, request);
    }

    @PatchMapping("/{id}/resolver")
    public ChamadoResponse resolver(@PathVariable Long id, @Valid @RequestBody ResolverChamadoRequest request) {
        return chamadoService.resolver(id, request);
    }

    @PatchMapping("/{id}/cancelar")
    public ChamadoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarChamadoRequest request) {
        return chamadoService.cancelar(id, request);
    }

    @PostMapping("/{id}/interacoes")
    public ResponseEntity<InteracaoResponse> registrarInteracao(@PathVariable Long id,
            @Valid @RequestBody InteracaoRequest request) {
        return ResponseEntity.status(201).body(chamadoService.registrarInteracao(id, request));
    }
}
