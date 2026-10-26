package br.edu.ifrn.chamados.dto.chamado;

import java.time.LocalDateTime;

import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;

public record ChamadoResumoResponse(
        Long id, String titulo, String local, Prioridade prioridade, StatusChamado status,
        LocalDateTime dataAbertura, LocalDateTime dataFechamento,
        Long solicitanteId, Long categoriaId, Long tecnicoId) {
}
