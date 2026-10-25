package br.edu.ifrn.chamados;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.exception.ApiExceptionHandler;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import jakarta.validation.Valid;

class ApiExceptionHandlerTests {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void configurar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerTeste())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void fechar() {
        validator.close();
    }

    @Test
    void deveRetornar400ComOsCamposInvalidos() throws Exception {
        mockMvc.perform(post("/teste/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":" ","email":"invalido","matricula":"123","tipo":"SOLICITANTE"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem", containsString("nome")))
                .andExpect(jsonPath("$.mensagem", containsString("email")))
                .andExpect(jsonPath("$.caminho").value("/teste/usuarios"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"tipo\":\"ADMIN\"}"})
    void deveRetornar400ParaJsonOuEnumInvalido(String json) throws Exception {
        mockMvc.perform(post("/teste/usuarios").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("JSON invalido ou campo com valor incompativel"));
    }

    @Test
    void deveRetornar404ParaRecursoInexistente() throws Exception {
        mockMvc.perform(get("/teste/recursos/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Usuario nao encontrado"))
                .andExpect(jsonPath("$.caminho").value("/teste/recursos/1"));
    }

    @Test
    void deveRetornar409ParaRegraDeNegocio() throws Exception {
        mockMvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Email ja cadastrado"));
    }

    @Test
    void deveRetornar409SemExporDetalhesDoBanco() throws Exception {
        mockMvc.perform(get("/teste/integridade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Operacao viola uma restricao dos dados cadastrados"));
    }

    @Test
    void devePadronizarParametroComTipoInvalido() throws Exception {
        mockMvc.perform(get("/teste/recursos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.caminho").value("/teste/recursos/abc"));
    }

    @Test
    void devePadronizarMetodoNaoPermitido() throws Exception {
        mockMvc.perform(get("/teste/usuarios"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.mensagem").value("Metodo HTTP nao permitido"));
    }

    @Test
    void deveAceitarRequisicaoValida() throws Exception {
        mockMvc.perform(post("/teste/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Ana","email":"ana@example.com","matricula":"123","tipo":"SOLICITANTE"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @TestComponent
    @RestController
    static class ControllerTeste {

        @PostMapping("/teste/usuarios")
        public UsuarioRequest cadastrar(@Valid @RequestBody UsuarioRequest request) {
            return request;
        }

        @GetMapping("/teste/recursos/{id}")
        public void buscar(@PathVariable Long id) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado");
        }

        @GetMapping("/teste/conflito")
        public void conflito() {
            throw new RegraNegocioException("Email ja cadastrado");
        }

        @GetMapping("/teste/integridade")
        public void integridade() {
            throw new DataIntegrityViolationException("Detalhes internos do SQL");
        }
    }
}
