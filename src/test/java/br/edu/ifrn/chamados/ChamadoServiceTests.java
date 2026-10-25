package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import br.edu.ifrn.chamados.service.ChamadoService;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTests {

    @Mock private ChamadoRepository chamados;
    @Mock private UsuarioRepository usuarios;
    @Mock private CategoriaRepository categorias;
    @Mock private InteracaoRepository interacoes;

    private ChamadoService service;
    private Usuario solicitante;
    private Usuario tecnico;
    private Categoria categoria;

    @BeforeEach
    void configurar() {
        service = new ChamadoService(chamados, usuarios, categorias, interacoes);
        Setor setor = new Setor();
        setor.setId(1L);
        solicitante = usuario(1L, TipoUsuario.SOLICITANTE, null);
        tecnico = usuario(2L, TipoUsuario.TECNICO, setor);
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setSetor(setor);
    }

    @Test
    void deveAbrirComStatusDataEPrioridadePadrao() {
        prepararAbertura();
        var response = service.abrir(abertura(null));
        assertEquals(StatusChamado.ABERTO, response.status());
        assertEquals(Prioridade.MEDIA, response.prioridade());
        assertNotNull(response.dataAbertura());
        assertNull(response.dataFechamento());
        assertNull(response.tecnicoId());
        assertEquals(0, response.interacoes().size());
    }

    @ParameterizedTest
    @EnumSource(Prioridade.class)
    void devePreservarPrioridadeInformada(Prioridade prioridade) {
        prepararAbertura();
        assertEquals(prioridade, service.abrir(abertura(prioridade)).prioridade());
    }

    @Test
    void deveRejeitarAberturaPorTecnico() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(tecnico));
        assertThrows(RegraNegocioException.class, () -> service.abrir(abertura(null)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveRejeitarSolicitanteInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.abrir(abertura(null)));
    }

    @Test
    void deveRejeitarCategoriaInexistente() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(solicitante));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.abrir(abertura(null)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveDetalharComInteracoes() {
        Chamado chamado = prepararChamado(StatusChamado.EM_ATENDIMENTO);
        Interacao interacao = new Interacao();
        interacao.setId(5L);
        interacao.setTexto("Verificando a rede");
        interacao.setAutor(tecnico);
        interacao.setDataHora(LocalDateTime.now());
        chamado.getInteracoes().add(interacao);
        var response = service.buscar(10L);
        assertEquals(2L, response.tecnicoId());
        assertEquals("Verificando a rede", response.interacoes().getFirst().texto());
        assertEquals(2L, response.interacoes().getFirst().autorId());
    }

    @Test
    void deveRetornarErroParaChamadoInexistenteEmTodasAsAcoes() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscar(99L));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.assumir(99L, new AssumirChamadoRequest(2L)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.resolver(99L, new ResolverChamadoRequest(2L, "Resolvido")));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cancelar(99L, new CancelarChamadoRequest(1L)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.registrarInteracao(99L, new InteracaoRequest("Teste", 1L)));
    }

    @Test
    void deveAssumirComTecnicoDoSetor() {
        prepararChamado(StatusChamado.ABERTO);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tecnico));
        permitirSalvar();
        var response = service.assumir(10L, new AssumirChamadoRequest(2L));
        assertEquals(StatusChamado.EM_ATENDIMENTO, response.status());
        assertEquals(2L, response.tecnicoId());
        assertNull(response.dataFechamento());
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
    void deveImpedirAssumirChamadoQueNaoEstaAberto(StatusChamado status) {
        prepararChamado(status);
        assertThrows(RegraNegocioException.class, () -> service.assumir(10L, new AssumirChamadoRequest(2L)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveImpedirTecnicoDeOutroSetor() {
        prepararChamado(StatusChamado.ABERTO);
        Setor outroSetor = new Setor();
        outroSetor.setId(2L);
        tecnico.setSetor(outroSetor);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tecnico));
        assertThrows(RegraNegocioException.class, () -> service.assumir(10L, new AssumirChamadoRequest(2L)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveImpedirSolicitanteDeAssumir() {
        prepararChamado(StatusChamado.ABERTO);
        when(usuarios.findById(1L)).thenReturn(Optional.of(solicitante));
        assertThrows(RegraNegocioException.class, () -> service.assumir(10L, new AssumirChamadoRequest(1L)));
    }

    @Test
    void deveImpedirTecnicoSemSetorDeAssumir() {
        prepararChamado(StatusChamado.ABERTO);
        tecnico.setSetor(null);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tecnico));
        assertThrows(RegraNegocioException.class, () -> service.assumir(10L, new AssumirChamadoRequest(2L)));
    }

    @Test
    void deveResolverComTecnicoAtribuidoESolucao() {
        prepararChamado(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tecnico));
        permitirSalvar();
        var response = service.resolver(10L, new ResolverChamadoRequest(2L, "Cabo substituido"));
        assertEquals(StatusChamado.RESOLVIDO, response.status());
        assertEquals("Cabo substituido", response.solucao());
        assertNotNull(response.dataFechamento());
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = "EM_ATENDIMENTO", mode = EnumSource.Mode.EXCLUDE)
    void deveImpedirResolverForaDeAtendimento(StatusChamado status) {
        prepararChamado(status);
        assertThrows(RegraNegocioException.class,
                () -> service.resolver(10L, new ResolverChamadoRequest(2L, "Cabo substituido")));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveImpedirOutroTecnicoDeResolver() {
        prepararChamado(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario(3L, TipoUsuario.TECNICO, tecnico.getSetor())));
        assertThrows(RegraNegocioException.class,
                () -> service.resolver(10L, new ResolverChamadoRequest(3L, "Cabo substituido")));
        verify(chamados, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void deveExigirSolucaoNoService(String solucao) {
        prepararChamado(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.findById(2L)).thenReturn(Optional.of(tecnico));
        assertThrows(RegraNegocioException.class,
                () -> service.resolver(10L, new ResolverChamadoRequest(2L, solucao)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveCancelarPeloProprioSolicitante() {
        prepararChamado(StatusChamado.ABERTO);
        when(usuarios.findById(1L)).thenReturn(Optional.of(solicitante));
        permitirSalvar();
        var response = service.cancelar(10L, new CancelarChamadoRequest(1L));
        assertEquals(StatusChamado.CANCELADO, response.status());
        assertNotNull(response.dataFechamento());
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
    void deveImpedirCancelarDepoisDaAbertura(StatusChamado status) {
        prepararChamado(status);
        assertThrows(RegraNegocioException.class, () -> service.cancelar(10L, new CancelarChamadoRequest(1L)));
        verify(chamados, never()).save(any());
    }

    @Test
    void deveImpedirCancelamentoPorOutroSolicitante() {
        prepararChamado(StatusChamado.ABERTO);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario(3L, TipoUsuario.SOLICITANTE, null)));
        assertThrows(RegraNegocioException.class, () -> service.cancelar(10L, new CancelarChamadoRequest(3L)));
        verify(chamados, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2})
    void devePermitirInteracoesDosParticipantes(Long autorId) {
        prepararChamado(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.findById(autorId)).thenReturn(Optional.of(autorId == 1 ? solicitante : tecnico));
        when(interacoes.save(any(Interacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        var response = service.registrarInteracao(10L, new InteracaoRequest("Verificando", autorId));
        assertEquals(autorId, response.autorId());
        assertEquals("Verificando", response.texto());
        assertNotNull(response.dataHora());
        assertEquals(1, service.buscar(10L).interacoes().size());
    }

    @Test
    void devePermitirInteracaoDoSolicitanteAntesDeAtribuirTecnico() {
        prepararChamado(StatusChamado.ABERTO);
        when(usuarios.findById(1L)).thenReturn(Optional.of(solicitante));
        when(interacoes.save(any(Interacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        assertEquals(1L, service.registrarInteracao(10L, new InteracaoRequest("Mais detalhes", 1L)).autorId());
    }

    @ParameterizedTest
    @EnumSource(value = TipoUsuario.class)
    void deveRejeitarInteracoesDeTerceiros(TipoUsuario tipo) {
        prepararChamado(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario(3L, tipo, tecnico.getSetor())));
        assertThrows(RegraNegocioException.class,
                () -> service.registrarInteracao(10L, new InteracaoRequest("Teste", 3L)));
        verify(interacoes, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"RESOLVIDO", "CANCELADO"})
    void deveBloquearInteracoesDepoisDoEncerramento(StatusChamado status) {
        prepararChamado(status);
        assertThrows(RegraNegocioException.class,
                () -> service.registrarInteracao(10L, new InteracaoRequest("Teste", 1L)));
        verify(interacoes, never()).save(any());
    }

    @Test
    void deveRetornarErroParaAutoresInexistentes() {
        Chamado chamado = prepararChamado(StatusChamado.ABERTO);
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.assumir(10L, new AssumirChamadoRequest(99L)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cancelar(10L, new CancelarChamadoRequest(99L)));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.registrarInteracao(10L, new InteracaoRequest("Teste", 99L)));
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.resolver(10L, new ResolverChamadoRequest(99L, "Resolvido")));
    }

    private void prepararAbertura() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(solicitante));
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        permitirSalvar();
    }

    private void permitirSalvar() {
        when(chamados.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private ChamadoRequest abertura(Prioridade prioridade) {
        return new ChamadoRequest("Sem rede", "Wi-Fi indisponivel", "Sala 1", prioridade, 1L, 1L);
    }

    private Chamado prepararChamado(StatusChamado status) {
        Chamado chamado = new Chamado();
        chamado.setId(10L);
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        chamado.setStatus(status);
        if (status == StatusChamado.EM_ATENDIMENTO || status == StatusChamado.RESOLVIDO) {
            chamado.setTecnico(tecnico);
        }
        when(chamados.findById(10L)).thenReturn(Optional.of(chamado));
        return chamado;
    }

    private Usuario usuario(Long id, TipoUsuario tipo, Setor setor) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipo(tipo);
        usuario.setSetor(setor);
        return usuario;
    }
}
