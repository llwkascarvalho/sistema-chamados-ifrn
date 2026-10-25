package br.edu.ifrn.chamados.dto.chamado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InteracaoRequest(
        @NotBlank(message = "Texto e obrigatorio") String texto,
        @NotNull(message = "Autor e obrigatorio") @Positive(message = "Autor deve ter um id positivo") Long autorId) {
}
