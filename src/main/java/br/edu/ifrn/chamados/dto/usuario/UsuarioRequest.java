package br.edu.ifrn.chamados.dto.usuario;

import br.edu.ifrn.chamados.model.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "Nome e obrigatorio") @Size(max = 100, message = "Nome deve ter ate 100 caracteres") String nome,
        @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido")
        @Size(max = 255, message = "Email deve ter ate 255 caracteres") String email,
        @NotBlank(message = "Matricula e obrigatoria") @Size(max = 30, message = "Matricula deve ter ate 30 caracteres") String matricula,
        @NotNull(message = "Tipo e obrigatorio") TipoUsuario tipo,
        @Positive(message = "Setor deve ter um id positivo") Long setorId) {
}
