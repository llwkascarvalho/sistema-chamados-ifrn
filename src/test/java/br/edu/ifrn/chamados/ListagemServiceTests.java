package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.edu.ifrn.chamados.dto.chamado.ChamadoFiltro;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import br.edu.ifrn.chamados.service.ChamadoService;

@ExtendWith(MockitoExtension.class)
class ListagemServiceTests {

    @Mock private ChamadoRepository chamados;
    @Mock private UsuarioRepository usuarios;
    @Mock private CategoriaRepository categorias;
    @Mock private InteracaoRepository interacoes;

    private ChamadoService service;

    @BeforeEach
    void configurar() {
        service = new ChamadoService(chamados, usuarios, categorias, interacoes);
    }

    @Test
    void deveCombinarFiltrosEPreservarMetadados() {
        var inicio = LocalDateTime.of(2026, 10, 1, 0, 0);
        var fim = inicio.plusDays(10);
        var pagina = PageRequest.of(1, 2);
        var filtro = new ChamadoFiltro(StatusChamado.ABERTO, Prioridade.ALTA, 1L, 2L, 3L, 4L, inicio, fim);
        when(chamados.listar(StatusChamado.ABERTO, Prioridade.ALTA, 1L, 2L, 3L, 4L, inicio, fim, pagina))
                .thenReturn(new PageImpl<>(List.of(chamado()), pagina, 3));

        var resultado = service.listar(filtro, pagina);
        assertEquals(1, resultado.getNumber());
        assertEquals(2, resultado.getSize());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
        assertEquals(10L, resultado.getContent().getFirst().id());
        assertNull(resultado.getContent().getFirst().tecnicoId());
    }

    @Test
    void deveIgnorarFiltrosOmitidos() {
        var pagina = PageRequest.of(0, 10);
        when(chamados.listar(null, null, null, null, null, null, null, null, pagina))
                .thenReturn(new PageImpl<>(List.of(), pagina, 0));
        var resultado = service.listar(new ChamadoFiltro(null, null, null, null, null, null, null, null), pagina);
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void deveRejeitarIntervaloInvertidoAntesDaConsulta() {
        var inicio = LocalDateTime.of(2026, 10, 10, 0, 0);
        var filtro = new ChamadoFiltro(null, null, null, null, null, null, inicio, inicio.minusDays(1));
        assertThrows(DadosInvalidosException.class, () -> service.listar(filtro, PageRequest.of(0, 10)));
        verify(chamados, never()).listar(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveAceitarIntervaloComLimitesIguais() {
        var data = LocalDateTime.of(2026, 10, 10, 0, 0);
        var pagina = PageRequest.of(0, 10);
        when(chamados.listar(null, null, null, null, null, null, data, data, pagina))
                .thenReturn(new PageImpl<>(List.of(), pagina, 0));
        assertEquals(0, service.listar(new ChamadoFiltro(null, null, null, null, null, null, data, data), pagina)
                .getTotalElements());
    }

    @Test
    void deveConsultarApenasChamadosAbertosPeloSolicitante() {
        var pagina = PageRequest.of(0, 10);
        Usuario usuario = new Usuario();
        usuario.setTipo(TipoUsuario.SOLICITANTE);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario));
        when(chamados.listarPorSolicitante(3L, StatusChamado.ABERTO, pagina))
                .thenReturn(new PageImpl<>(List.of(chamado()), pagina, 1));
        assertEquals(1, service.listarPorUsuario(3L, StatusChamado.ABERTO, pagina).getTotalElements());
        verify(chamados, never()).listarPorTecnico(any(), any(), any());
    }

    @Test
    void deveConsultarApenasChamadosAtribuidosAoTecnico() {
        var pagina = PageRequest.of(0, 10);
        Usuario usuario = new Usuario();
        usuario.setTipo(TipoUsuario.TECNICO);
        when(usuarios.findById(4L)).thenReturn(Optional.of(usuario));
        when(chamados.listarPorTecnico(4L, null, pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));
        assertEquals(0, service.listarPorUsuario(4L, null, pagina).getTotalElements());
        verify(chamados, never()).listarPorSolicitante(any(), any(), any());
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.listarPorUsuario(99L, null, PageRequest.of(0, 10)));
        verify(chamados, never()).listarPorTecnico(any(), any(), any());
        verify(chamados, never()).listarPorSolicitante(any(), any(), any());
    }

    private Chamado chamado() {
        Usuario solicitante = new Usuario();
        solicitante.setId(3L);
        Categoria categoria = new Categoria();
        categoria.setId(2L);
        Chamado chamado = new Chamado();
        chamado.setId(10L);
        chamado.setTitulo("Sem rede");
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setPrioridade(Prioridade.ALTA);
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        return chamado;
    }
}
