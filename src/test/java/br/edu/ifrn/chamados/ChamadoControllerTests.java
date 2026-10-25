package br.edu.ifrn.chamados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.edu.ifrn.chamados.controller.ChamadoController;
import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResponse;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoResponse;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.exception.ApiExceptionHandler;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.service.ChamadoService;

@ExtendWith(MockitoExtension.class)
class ChamadoControllerTests {

    @Mock
    private ChamadoService service;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void configurar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChamadoController(service))
                .setControllerAdvice(new ApiExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void fechar() {
        validator.close();
    }

    @Test
    void deveAbrirCom201ELocation() throws Exception {
        when(service.abrir(any())).thenReturn(resposta(StatusChamado.ABERTO));
        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Sem rede","descricao":"Wi-Fi indisponivel","local":"Sala 1",
                 "solicitanteId":1,"categoriaId":1}
                """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/chamados/10"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.dataAbertura").isNotEmpty());
    }

    @Test
    void deveDetalharChamadoComInteracoes() throws Exception {
        var data = LocalDateTime.now();
        when(service.buscar(10L)).thenReturn(new ChamadoResponse(10L, "Sem rede", "Wi-Fi", "Sala 1",
                Prioridade.MEDIA, StatusChamado.EM_ATENDIMENTO, data, null, null, 1L, 1L, 2L,
                List.of(new InteracaoResponse(5L, "Verificando", data, 2L))));
        mockMvc.perform(get("/chamados/10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.interacoes[0].texto").value("Verificando"))
                .andExpect(jsonPath("$.interacoes[0].autorId").value(2));
    }

    @Test
    void deveAssumirCom200() throws Exception {
        when(service.assumir(eq(10L), any())).thenReturn(resposta(StatusChamado.EM_ATENDIMENTO));
        mockMvc.perform(patch("/chamados/10/assumir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
        verify(service).assumir(10L, new AssumirChamadoRequest(2L));
    }

    @Test
    void deveResolverCom200() throws Exception {
        when(service.resolver(eq(10L), any())).thenReturn(resposta(StatusChamado.RESOLVIDO));
        mockMvc.perform(patch("/chamados/10/resolver").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":2,\"solucao\":\"Cabo substituido\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVIDO"));
        verify(service).resolver(10L, new ResolverChamadoRequest(2L, "Cabo substituido"));
    }

    @Test
    void deveCancelarCom200() throws Exception {
        when(service.cancelar(eq(10L), any())).thenReturn(resposta(StatusChamado.CANCELADO));
        mockMvc.perform(patch("/chamados/10/cancelar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitanteId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        verify(service).cancelar(10L, new CancelarChamadoRequest(1L));
    }

    @Test
    void deveRegistrarInteracaoCom201() throws Exception {
        when(service.registrarInteracao(eq(10L), any()))
                .thenReturn(new InteracaoResponse(5L, "Mais detalhes", LocalDateTime.now(), 1L));
        mockMvc.perform(post("/chamados/10/interacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Mais detalhes\",\"autorId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.dataHora").isNotEmpty());
        verify(service).registrarInteracao(10L, new InteracaoRequest("Mais detalhes", 1L));
    }

    @Test
    void deveRejeitarCamposObrigatoriosAntesDoService() throws Exception {
        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        for (String acao : List.of("assumir", "resolver", "cancelar")) {
            mockMvc.perform(patch("/chamados/10/" + acao).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/chamados/10/interacoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarSolucaoEInteracaoVazias() throws Exception {
        mockMvc.perform(patch("/chamados/10/resolver").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":2,\"solucao\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/chamados/10/interacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"autorId\":1,\"texto\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarPrioridadeInvalida() throws Exception {
        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"prioridade\":\"URGENTE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar404Padronizado() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado nao encontrado"));
        mockMvc.perform(get("/chamados/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.caminho").value("/chamados/99"));
    }

    @Test
    void deveRetornar409ParaViolacaoDeRegra() throws Exception {
        when(service.assumir(eq(10L), any())).thenThrow(new RegraNegocioException("Tecnico de outro setor"));
        mockMvc.perform(patch("/chamados/10/assumir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Tecnico de outro setor"))
                .andExpect(jsonPath("$.caminho").value("/chamados/10/assumir"));
    }

    private ChamadoResponse resposta(StatusChamado status) {
        return new ChamadoResponse(10L, "Sem rede", "Wi-Fi indisponivel", "Sala 1", Prioridade.MEDIA,
                status, LocalDateTime.now(), null, null, 1L, 1L, null, List.of());
    }
}
