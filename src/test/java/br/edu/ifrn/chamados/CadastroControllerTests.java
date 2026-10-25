package br.edu.ifrn.chamados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.edu.ifrn.chamados.controller.CategoriaController;
import br.edu.ifrn.chamados.controller.SetorController;
import br.edu.ifrn.chamados.controller.UsuarioController;
import br.edu.ifrn.chamados.dto.categoria.CategoriaResponse;
import br.edu.ifrn.chamados.dto.setor.SetorResponse;
import br.edu.ifrn.chamados.dto.usuario.UsuarioResponse;
import br.edu.ifrn.chamados.exception.ApiExceptionHandler;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.SetorService;
import br.edu.ifrn.chamados.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class CadastroControllerTests {

    @Mock private SetorService setores;
    @Mock private CategoriaService categorias;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new SetorController(setores, categorias),
                new CategoriaController(categorias), new UsuarioController(usuarios))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(pageable)
                .setValidator(validator).build();
    }

    @AfterEach
    void fechar() {
        validator.close();
    }

    @Test
    void deveCriarSetorComStatus201ELocation() throws Exception {
        when(setores.criar(any())).thenReturn(new SetorResponse(1L, "TI", "COTIC"));
        mockMvc.perform(post("/setores").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"TI","sigla":"COTIC"}
                        """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/setores/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deveCriarCategoriaComStatus201ELocation() throws Exception {
        when(categorias.criar(any())).thenReturn(new CategoriaResponse(2L, "Rede", "Wi-Fi", 1L));
        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Rede","descricao":"Wi-Fi","setorId":1}
                        """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/categorias/2"))
                .andExpect(jsonPath("$.setorId").value(1));
    }

    @Test
    void deveCriarUsuarioComStatus201ELocation() throws Exception {
        when(usuarios.criar(any())).thenReturn(
                new UsuarioResponse(3L, "Ana", "ana@example.com", "123", TipoUsuario.SOLICITANTE, null));
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@example.com","matricula":"123","tipo":"SOLICITANTE"}
                        """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/usuarios/3"));
    }

    @Test
    void deveValidarEntradaAntesDeChamarService() throws Exception {
        for (String rota : List.of("/setores", "/categorias", "/usuarios")) {
            mockMvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.caminho").value(rota));
        }
        verifyNoInteractions(setores, categorias, usuarios);
    }

    @Test
    void deveAtualizarCadastrosComStatus200() throws Exception {
        mockMvc.perform(put("/setores/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"TI\",\"sigla\":\"COTIC\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/categorias/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Rede\",\"descricao\":\"Wi-Fi\",\"setorId\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/usuarios/3").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@example.com","matricula":"123","tipo":"SOLICITANTE"}
                        """))
                .andExpect(status().isOk());
        verify(setores).atualizar(eq(1L), any());
        verify(categorias).atualizar(eq(2L), any());
        verify(usuarios).atualizar(eq(3L), any());
    }

    @Test
    void deveExcluirSetorECategoriaComStatus204() throws Exception {
        mockMvc.perform(delete("/setores/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/categorias/2")).andExpect(status().isNoContent());
        verify(setores).excluir(1L);
        verify(categorias).excluir(2L);
    }

    @Test
    void naoDeveExporDeleteDeUsuario() throws Exception {
        mockMvc.perform(delete("/usuarios/1")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(usuarios);
    }

    @Test
    void deveDevolverMetadadosDaPaginaEEncaminharFiltro() throws Exception {
        var pagina = PageRequest.of(1, 2);
        when(setores.listar("ti", pagina)).thenReturn(
                new PageImpl<>(List.of(new SetorResponse(3L, "TI", "COTIC")), pagina, 3));
        mockMvc.perform(get("/setores?nome=ti&page=1&size=2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void deveEncaminharFiltrosCombinadosDeUsuarios() throws Exception {
        when(usuarios.listar(eq("Ana"), eq(TipoUsuario.TECNICO), eq(1L), any(Pageable.class)))
                .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(3), 0));
        mockMvc.perform(get("/usuarios?nome=Ana&tipo=TECNICO&setorId=1"))
                .andExpect(status().isOk());
        verify(usuarios).listar("Ana", TipoUsuario.TECNICO, 1L, PageRequest.of(0, 10));
    }

    @Test
    void deveEncaminharFiltrosDeCategoriasELimitarTamanho() throws Exception {
        when(categorias.listar(eq("Rede"), eq(1L), any(Pageable.class)))
                .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(2), 0));
        mockMvc.perform(get("/categorias?nome=Rede&setorId=1&size=100"))
                .andExpect(status().isOk());
        verify(categorias).listar("Rede", 1L, PageRequest.of(0, 50));
    }

    @Test
    void deveListarCategoriasDoSetor() throws Exception {
        when(categorias.listarPorSetor(eq(1L), isNull(), any(Pageable.class)))
                .thenAnswer(invocacao -> new PageImpl<>(List.of(), invocacao.getArgument(2), 0));
        mockMvc.perform(get("/setores/1/categorias"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(categorias).listarPorSetor(1L, null, PageRequest.of(0, 10));
    }

    @Test
    void deveRetornar404ParaCadastroInexistente() throws Exception {
        when(categorias.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Categoria nao encontrada"));
        mockMvc.perform(get("/categorias/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRetornar409ParaEmailDuplicado() throws Exception {
        when(usuarios.criar(any())).thenThrow(new RegraNegocioException("Email ja cadastrado"));
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@example.com","matricula":"123","tipo":"SOLICITANTE"}
                        """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Email ja cadastrado"));
    }
}
