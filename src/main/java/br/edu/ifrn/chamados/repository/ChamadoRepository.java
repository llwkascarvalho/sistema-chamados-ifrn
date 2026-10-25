package br.edu.ifrn.chamados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifrn.chamados.model.Chamado;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);
}
