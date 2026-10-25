package br.edu.ifrn.chamados.dto.chamado;

import java.time.LocalDateTime;

public record InteracaoResponse(Long id, String texto, LocalDateTime dataHora, Long autorId) {
}
