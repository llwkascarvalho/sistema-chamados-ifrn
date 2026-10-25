package br.edu.ifrn.chamados.dto.chamado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CancelarChamadoRequest(
        @NotNull(message = "Solicitante e obrigatorio") @Positive(message = "Solicitante deve ter um id positivo") Long solicitanteId) {
}
