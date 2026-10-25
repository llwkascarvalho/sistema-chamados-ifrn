package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.model.TipoUsuario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ValidacaoDtosTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fechar() {
        factory.close();
    }

    @ParameterizedTest
    @MethodSource("requisicoesInvalidas")
    void deveRejeitarDadosInvalidos(Object requisicao) {
        assertFalse(validator.validate(requisicao).isEmpty());
    }

    static Stream<Object> requisicoesInvalidas() {
        return Stream.of(
                new SetorRequest(" ", "COTIC"),
                new SetorRequest("TI", "A".repeat(21)),
                new CategoriaRequest("Rede", "Descricao", null),
                new UsuarioRequest("Ana", "email-invalido", "123", TipoUsuario.SOLICITANTE, null),
                new UsuarioRequest("Ana", "ana@example.com", "123", null, null),
                new ChamadoRequest("Titulo", "Descricao", "Sala", null, 0L, 1L),
                new ChamadoRequest("Titulo", "Descricao", "A".repeat(201), null, 1L, 1L),
                new AssumirChamadoRequest(null),
                new ResolverChamadoRequest(1L, " "),
                new CancelarChamadoRequest(-1L),
                new InteracaoRequest(" ", 1L));
    }

    @Test
    void deveAceitarAberturaSemPrioridade() {
        var requisicao = new ChamadoRequest("Wi-Fi", "Sem conexao", "Sala 1", null, 1L, 1L);
        assertTrue(validator.validate(requisicao).isEmpty());
    }

    @Test
    void deveAceitarSolicitanteSemSetor() {
        var requisicao = new UsuarioRequest("Ana", "ana@example.com", "123", TipoUsuario.SOLICITANTE, null);
        assertTrue(validator.validate(requisicao).isEmpty());
    }
}
