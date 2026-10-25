package br.edu.ifrn.chamados.dto.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "Nome e obrigatorio") @Size(max = 100, message = "Nome deve ter ate 100 caracteres") String nome,
        @NotBlank(message = "Descricao e obrigatoria") String descricao,
        @NotNull(message = "Setor e obrigatorio") @Positive(message = "Setor deve ter um id positivo") Long setorId) {
}
