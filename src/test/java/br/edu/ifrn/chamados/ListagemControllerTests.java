package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.edu.ifrn.chamados.controller.ChamadoController;
import br.edu.ifrn.chamados.controller.UsuarioController;
import br.edu.ifrn.chamados.dto.chamado.ChamadoFiltro;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResumoResponse;
import br.edu.ifrn.chamados.exception.ApiExceptionHandler;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.service.ChamadoService;
import br.edu.ifrn.chamados.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class ListagemControllerTests {

    @Mock private ChamadoService chamados;
    @Mock private UsuarioService usuarios;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void configurar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var pageable = new PageableHandlerMethodArgumentResolver();
        pageable.setFallbackPageable(PageRequest.of(0, 10));
        pageable.setMaxPageSize(50);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChamadoController(chamados),
                new UsuarioController(usuarios, chamados))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(pageable).setValidator(validator).build();
    }

    @AfterEach
    void fechar() {
        validator.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/chamados", "/usuarios/1/chamados"})
    void deveAplicarPaginacaoPadrao(String rota) throws Exception {
        prepararPaginaVazia(rota);
        mockMvc.perform(get(rota)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10)).andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/chamados", "/usuarios/1/chamados"})
    void deveLimitarPaginaACinquenta(String rota) throws Exception {
        prepararPaginaVazia(rota);
        mockMvc.perform(get(rota).param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void deveRetornarResumoComMetadadosEOrdenacao() throws Exception {
        var pagina = PageRequest.of(2, 3, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        var resumo = new ChamadoResumoResponse(10L, "Sem rede", "Sala 1", Prioridade.ALTA,
                StatusChamado.ABERTO, LocalDateTime.now(), null, 1L, 2L, null);
        when(chamados.listar(any(), eq(pagina))).thenReturn(new PageImpl<>(List.of(resumo), pagina, 7));
        mockMvc.perform(get("/chamados?page=2&size=3&sort=dataAbertura,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].interacoes").doesNotExist())
                .andExpect(jsonPath("$.number").value(2)).andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(7)).andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void deveReceberOsOitoFiltrosCombinados() throws Exception {
        when(chamados.listar(any(), any(Pageable.class))).thenAnswer(
                invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(1), 0));
        mockMvc.perform(get("/chamados").param("status", "EM_ATENDIMENTO").param("prioridade", "ALTA")
                .param("setorId", "1").param("categoriaId", "2").param("solicitanteId", "3")
                .param("tecnicoId", "4").param("dataInicio", "2026-10-01T00:00:00")
                .param("dataFim", "2026-10-26T23:59:59"))
                .andExpect(status().isOk());
        var filtro = ArgumentCaptor.forClass(ChamadoFiltro.class);
        verify(chamados).listar(filtro.capture(), eq(PageRequest.of(0, 10)));
        assertEquals(new ChamadoFiltro(StatusChamado.EM_ATENDIMENTO, Prioridade.ALTA, 1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 26, 23, 59, 59)), filtro.getValue());
    }

    @Test
    void deveFiltrarChamadosDoUsuarioPorStatus() throws Exception {
        when(chamados.listarPorUsuario(eq(1L), eq(StatusChamado.RESOLVIDO), any(Pageable.class)))
                .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(2), 0));
        mockMvc.perform(get("/usuarios/1/chamados?status=RESOLVIDO"))
                .andExpect(status().isOk());
        verify(chamados).listarPorUsuario(1L, StatusChamado.RESOLVIDO, PageRequest.of(0, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"status=INVALIDO", "prioridade=URGENTE", "setorId=-1", "categoriaId=abc",
            "solicitanteId=0", "tecnicoId=-2", "dataInicio=ontem", "dataFim=2026-13-40"})
    void deveRejeitarFiltrosInvalidosAntesDoService(String query) throws Exception {
        mockMvc.perform(get("/chamados?" + query))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.caminho").value("/chamados"));
        verifyNoInteractions(chamados);
    }

    @Test
    void deveRejeitarStatusInvalidoNaConsultaPorUsuario() throws Exception {
        mockMvc.perform(get("/usuarios/1/chamados?status=INVALIDO"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chamados);
    }

    @Test
    void devePadronizarErroDeIntervaloInvertido() throws Exception {
        when(chamados.listar(any(), any())).thenThrow(new DadosInvalidosException("Periodo invalido"));
        mockMvc.perform(get("/chamados?dataInicio=2026-10-10T00:00:00&dataFim=2026-10-01T00:00:00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value("Periodo invalido"));
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        when(chamados.listarPorUsuario(eq(99L), isNull(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Usuario nao encontrado"));
        mockMvc.perform(get("/usuarios/99/chamados"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.caminho").value("/usuarios/99/chamados"));
    }

    private void prepararPaginaVazia(String rota) {
        if (rota.equals("/chamados")) {
            when(chamados.listar(any(), any(Pageable.class)))
                    .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(1), 0));
        } else {
            when(chamados.listarPorUsuario(eq(1L), isNull(), any(Pageable.class)))
                    .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(2), 0));
        }
    }
}
