package br.edu.ifrn.chamados.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @Query("""
            select c from Chamado c
            where (:status is null or c.status = :status)
              and (:prioridade is null or c.prioridade = :prioridade)
              and (:setorId is null or c.categoria.setor.id = :setorId)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
              and (:solicitanteId is null or c.solicitante.id = :solicitanteId)
              and (:tecnicoId is null or c.tecnico.id = :tecnicoId)
              and (cast(:dataInicio as timestamp) is null or c.dataAbertura >= :dataInicio)
              and (cast(:dataFim as timestamp) is null or c.dataAbertura <= :dataFim)
            """)
    Page<Chamado> listar(StatusChamado status, Prioridade prioridade, Long setorId, Long categoriaId,
            Long solicitanteId, Long tecnicoId, LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);
}
