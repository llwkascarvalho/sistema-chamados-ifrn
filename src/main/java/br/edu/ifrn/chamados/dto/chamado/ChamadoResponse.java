package br.edu.ifrn.chamados.dto.chamado;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;

public record ChamadoResponse(
        Long id, String titulo, String descricao, String local, Prioridade prioridade,
        StatusChamado status, LocalDateTime dataAbertura, LocalDateTime dataFechamento,
        String solucao, Long solicitanteId, Long categoriaId, Long tecnicoId,
        List<InteracaoResponse> interacoes) {
}
