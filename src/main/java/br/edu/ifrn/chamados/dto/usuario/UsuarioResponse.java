package br.edu.ifrn.chamados.dto.usuario;

import br.edu.ifrn.chamados.model.TipoUsuario;

public record UsuarioResponse(Long id, String nome, String email, String matricula, TipoUsuario tipo, Long setorId) {
}
