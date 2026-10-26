package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoFiltro;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResumoResponse;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.ChamadoService;
import br.edu.ifrn.chamados.service.SetorService;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ListagemPersistenciaTests {

    private final ChamadoService chamados;
    private final SetorService setores;
    private final CategoriaService categorias;
    private final UsuarioService usuarios;
    private final EntityManager entityManager;
    private final WebApplicationContext contexto;

    private Long setor1;
    private Long setor2;
    private Long categoria1;
    private Long categoria2;
    private Long solicitante1;
    private Long solicitante2;
    private Long tecnico1;
    private Long tecnico2;
    private Long aberto;
    private Long atendimento;
    private Long resolvido;
    private Long cancelado;
    private MockMvc mockMvc;

    @Autowired
    ListagemPersistenciaTests(ChamadoService chamados, SetorService setores, CategoriaService categorias,
            UsuarioService usuarios, EntityManager entityManager, WebApplicationContext contexto) {
        this.chamados = chamados;
        this.setores = setores;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.entityManager = entityManager;
        this.contexto = contexto;
    }

    @BeforeEach
    void prepararChamados() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        setor1 = setores.criar(new SetorRequest("Setor de rede teste", "TEST-LIST-1")).id();
        setor2 = setores.criar(new SetorRequest("Setor de manutencao teste", "TEST-LIST-2")).id();
        categoria1 = categorias.criar(new CategoriaRequest("Rede teste", "Wi-Fi", setor1)).id();
        categoria2 = categorias.criar(new CategoriaRequest("Eletrica teste", "Tomadas", setor2)).id();
        solicitante1 = criarUsuario("Solicitante 1", "lista.solicitante1@example.com", TipoUsuario.SOLICITANTE, null);
        solicitante2 = criarUsuario("Solicitante 2", "lista.solicitante2@example.com", TipoUsuario.SOLICITANTE, null);
        tecnico1 = criarUsuario("Tecnico 1", "lista.tecnico1@example.com", TipoUsuario.TECNICO, setor1);
        tecnico2 = criarUsuario("Tecnico 2", "lista.tecnico2@example.com", TipoUsuario.TECNICO, setor2);

        aberto = abrir(solicitante1, categoria1, Prioridade.ALTA, 1);
        atendimento = abrir(solicitante1, categoria1, Prioridade.MEDIA, 5);
        chamados.assumir(atendimento, new AssumirChamadoRequest(tecnico1));
        resolvido = abrir(solicitante2, categoria2, Prioridade.BAIXA, 20);
        chamados.assumir(resolvido, new AssumirChamadoRequest(tecnico2));
        chamados.resolver(resolvido, new ResolverChamadoRequest(tecnico2, "Tomada substituida"));
        cancelado = abrir(solicitante1, categoria1, Prioridade.ALTA, 10);
        chamados.cancelar(cancelado, new CancelarChamadoRequest(solicitante1));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deveCombinarTodosOsFiltros() {
        var filtro = new ChamadoFiltro(StatusChamado.EM_ATENDIMENTO, Prioridade.MEDIA, setor1, categoria1,
                solicitante1, tecnico1, data(5), data(5));
        assertEquals(List.of(atendimento), ids(chamados.listar(filtro, pagina())));
        var contraditorio = new ChamadoFiltro(StatusChamado.EM_ATENDIMENTO, Prioridade.ALTA, setor1, categoria1,
                solicitante1, tecnico1, data(5), data(5));
        assertEquals(0, chamados.listar(contraditorio, pagina()).getTotalElements());
    }

    @Test
    void deveAplicarOsFiltrosDeStatusPrioridadeESetor() {
        assertEquals(List.of(aberto), ids(chamados.listar(new ChamadoFiltro(
                StatusChamado.ABERTO, null, setor1, null, null, null, null, null), pagina())));
        assertEquals(List.of(aberto, cancelado), ids(chamados.listar(new ChamadoFiltro(
                null, Prioridade.ALTA, setor1, null, null, null, null, null), pagina())));
        assertEquals(List.of(resolvido), ids(chamados.listar(new ChamadoFiltro(
                null, null, setor2, null, null, null, null, null), pagina())));
        assertEquals(List.of(aberto, atendimento, cancelado), ids(chamados.listar(new ChamadoFiltro(
                null, null, null, categoria1, null, null, null, null), pagina())));
        assertEquals(0, chamados.listar(new ChamadoFiltro(
                null, null, setor1, categoria2, null, null, null, null), pagina()).getTotalElements());
    }

    @Test
    void deveFiltrarPorSolicitanteETecnico() {
        assertEquals(List.of(resolvido), ids(chamados.listar(new ChamadoFiltro(
                null, null, null, null, solicitante2, null, null, null), pagina())));
        assertEquals(List.of(atendimento), ids(chamados.listar(new ChamadoFiltro(
                null, null, null, null, null, tecnico1, null, null), pagina())));
    }

    @Test
    void deveIgnorarFiltrosOmitidosEIncluirChamadosSemTecnico() {
        var resultado = chamados.listar(new ChamadoFiltro(null, null, null, null, null, null, null, null),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "id")));
        assertTrue(ids(resultado).containsAll(List.of(aberto, atendimento, resolvido, cancelado)));
        assertNull(resultado.stream().filter(c -> c.id().equals(aberto)).findFirst().orElseThrow().tecnicoId());
    }

    @Test
    void deveAplicarPeriodoInclusivoEPermitirUmUnicoLimite() {
        assertEquals(List.of(atendimento, cancelado), ids(chamados.listar(new ChamadoFiltro(
                null, null, setor1, null, null, null, data(5), data(10)), pagina())));
        assertEquals(List.of(atendimento, cancelado), ids(chamados.listar(new ChamadoFiltro(
                null, null, setor1, null, null, null, data(5), null), pagina())));
        assertEquals(List.of(aberto, atendimento), ids(chamados.listar(new ChamadoFiltro(
                null, null, setor1, null, null, null, null, data(5)), pagina())));
        assertEquals(List.of(atendimento), ids(chamados.listar(new ChamadoFiltro(
                null, null, setor1, null, null, null, data(5), data(5)), pagina())));
    }

    @Test
    void deveConsultarChamadosPeloPapelDoUsuarioEStatus() {
        assertEquals(List.of(aberto, atendimento, cancelado), ids(chamados.listarPorUsuario(solicitante1, null, pagina())));
        assertEquals(List.of(cancelado), ids(chamados.listarPorUsuario(solicitante1, StatusChamado.CANCELADO, pagina())));
        assertEquals(List.of(atendimento), ids(chamados.listarPorUsuario(tecnico1, null, pagina())));
        assertEquals(List.of(resolvido), ids(chamados.listarPorUsuario(tecnico2, StatusChamado.RESOLVIDO, pagina())));
        assertEquals(0, chamados.listarPorUsuario(tecnico1, StatusChamado.ABERTO, pagina()).getTotalElements());
    }

    @Test
    void deveOrdenarPaginarEContarSemPerderRegistros() {
        var filtro = new ChamadoFiltro(null, null, setor1, null, null, null, null, null);
        var resultado = chamados.listar(filtro, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "dataAbertura")));
        assertEquals(List.of(atendimento), ids(resultado));
        assertEquals(1, resultado.getNumber());
        assertEquals(1, resultado.getSize());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
        var vazia = chamados.listar(filtro, PageRequest.of(10, 1));
        assertTrue(vazia.isEmpty());
        assertEquals(3, vazia.getTotalElements());
    }

    @Test
    void deveAplicarPaginacaoRealEmTodasAsListagens() throws Exception {
        var rotas = List.of("/setores", "/categorias", "/usuarios", "/setores/" + setor1 + "/categorias",
                "/chamados", "/usuarios/" + solicitante1 + "/chamados");
        for (String rota : rotas) {
            mockMvc.perform(get(rota)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10)).andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.totalPages").isNumber());
            mockMvc.perform(get(rota).param("size", "100"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
            mockMvc.perform(get(rota).param("page", "1").param("size", "1").param("sort", "id,desc"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(1));
        }
    }

    @Test
    void deveRetornarResumoSemInteracoesE400ParaPeriodoInvertido() throws Exception {
        mockMvc.perform(get("/chamados").param("setorId", setor1.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].interacoes").doesNotExist());
        mockMvc.perform(get("/chamados?dataInicio=2026-10-10T00:00:00&dataFim=2026-10-01T00:00:00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        mockMvc.perform(get("/usuarios/" + Long.MAX_VALUE + "/chamados"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private Long criarUsuario(String nome, String email, TipoUsuario tipo, Long setorId) {
        return usuarios.criar(new UsuarioRequest(nome, email, "20260100", tipo, setorId)).id();
    }

    private Long abrir(Long solicitanteId, Long categoriaId, Prioridade prioridade, int dia) {
        Long id = chamados.abrir(new ChamadoRequest("Teste de listagem", "Problema", "Sala 1", prioridade,
                solicitanteId, categoriaId)).id();
        entityManager.find(Chamado.class, id).setDataAbertura(data(dia));
        return id;
    }

    private LocalDateTime data(int dia) {
        return LocalDateTime.of(2026, 10, dia, 8, 0);
    }

    private PageRequest pagina() {
        return PageRequest.of(0, 50, Sort.by("id"));
    }

    private List<Long> ids(Page<ChamadoResumoResponse> pagina) {
        return pagina.stream().map(ChamadoResumoResponse::id).toList();
    }
}
