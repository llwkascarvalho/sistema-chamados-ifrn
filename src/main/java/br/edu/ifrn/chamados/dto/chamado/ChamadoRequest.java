package br.edu.ifrn.chamados.dto.chamado;

import br.edu.ifrn.chamados.model.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
        @NotBlank(message = "Titulo e obrigatorio") @Size(max = 200, message = "Titulo deve ter ate 200 caracteres") String titulo,
        @NotBlank(message = "Descricao e obrigatoria") String descricao,
        @NotBlank(message = "Local e obrigatorio") @Size(max = 200, message = "Local deve ter ate 200 caracteres") String local,
        Prioridade prioridade,
        @NotNull(message = "Solicitante e obrigatorio") @Positive(message = "Solicitante deve ter um id positivo") Long solicitanteId,
        @NotNull(message = "Categoria e obrigatoria") @Positive(message = "Categoria deve ter um id positivo") Long categoriaId) {
}
