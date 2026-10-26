package br.edu.ifrn.chamados.dto.chamado;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import jakarta.validation.constraints.Positive;

public record ChamadoFiltro(
        StatusChamado status,
        Prioridade prioridade,
        @Positive(message = "Setor deve ter um id positivo") Long setorId,
        @Positive(message = "Categoria deve ter um id positivo") Long categoriaId,
        @Positive(message = "Solicitante deve ter um id positivo") Long solicitanteId,
        @Positive(message = "Tecnico deve ter um id positivo") Long tecnicoId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
}
