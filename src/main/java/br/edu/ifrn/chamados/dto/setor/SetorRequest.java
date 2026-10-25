package br.edu.ifrn.chamados.dto.setor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetorRequest(
        @NotBlank(message = "Nome e obrigatorio") @Size(max = 100, message = "Nome deve ter ate 100 caracteres") String nome,
        @NotBlank(message = "Sigla e obrigatoria") @Size(max = 20, message = "Sigla deve ter ate 20 caracteres") String sigla) {
}
