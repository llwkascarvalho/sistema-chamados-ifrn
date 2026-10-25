package br.edu.ifrn.chamados.dto.chamado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssumirChamadoRequest(
        @NotNull(message = "Tecnico e obrigatorio") @Positive(message = "Tecnico deve ter um id positivo") Long tecnicoId) {
}
