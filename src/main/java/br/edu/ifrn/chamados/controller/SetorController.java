package br.edu.ifrn.chamados.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.setor.SetorResponse;
import br.edu.ifrn.chamados.service.SetorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/setores")
@RequiredArgsConstructor
public class SetorController {

    private final SetorService setorService;

    @GetMapping
    public Page<SetorResponse> listar(@RequestParam(required = false) String nome, Pageable pageable) {
        return setorService.listar(nome, pageable);
    }

    @GetMapping("/{id}")
    public SetorResponse buscar(@PathVariable Long id) {
        return setorService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<SetorResponse> criar(@Valid @RequestBody SetorRequest request) {
        SetorResponse response = setorService.criar(request);
        return ResponseEntity.created(URI.create("/setores/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
        return setorService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        setorService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
