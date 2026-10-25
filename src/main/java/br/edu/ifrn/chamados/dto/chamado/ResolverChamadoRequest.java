package br.edu.ifrn.chamados.dto.chamado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResolverChamadoRequest(
        @NotNull(message = "Tecnico e obrigatorio") @Positive(message = "Tecnico deve ter um id positivo") Long tecnicoId,
        @NotBlank(message = "Solucao e obrigatoria") String solucao) {
}
