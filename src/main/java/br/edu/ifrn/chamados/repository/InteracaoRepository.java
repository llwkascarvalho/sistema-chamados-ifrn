package br.edu.ifrn.chamados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifrn.chamados.model.Interacao;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {
}
