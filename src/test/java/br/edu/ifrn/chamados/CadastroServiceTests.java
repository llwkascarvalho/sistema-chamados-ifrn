package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.SetorService;
import br.edu.ifrn.chamados.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class CadastroServiceTests {

    @Mock private SetorRepository setores;
    @Mock private CategoriaRepository categorias;
    @Mock private UsuarioRepository usuarios;
    @Mock private ChamadoRepository chamados;

    private SetorService setorService;
    private CategoriaService categoriaService;
    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        setorService = new SetorService(setores, categorias, usuarios, chamados);
        categoriaService = new CategoriaService(categorias, setores, chamados);
        usuarioService = new UsuarioService(usuarios, setores);
    }

    @Test
    void deveRejeitarSiglaDuplicada() {
        when(setores.existsBySiglaIgnoreCase("COTIC")).thenReturn(true);
        assertThrows(RegraNegocioException.class,
                () -> setorService.criar(new SetorRequest("TI", "COTIC")));
        verify(setores, never()).save(any());
    }

    @Test
    void deveManterSiglaDoProprioSetorNaAtualizacao() {
        Setor setor = new Setor();
        setor.setId(1L);
        when(setores.findById(1L)).thenReturn(Optional.of(setor));
        when(setores.save(setor)).thenReturn(setor);
        assertEquals("COTIC", setorService.atualizar(1L, new SetorRequest("TI", "COTIC")).sigla());
        verify(setores).existsBySiglaIgnoreCaseAndIdNot("COTIC", 1L);
    }

    @Test
    void deveRejeitarSiglaDeOutroSetorNaAtualizacao() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(setores.existsBySiglaIgnoreCaseAndIdNot("COTIC", 1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class,
                () -> setorService.atualizar(1L, new SetorRequest("TI", "COTIC")));
        verify(setores, never()).save(any());
    }

    @Test
    void deveImpedirExclusaoDeSetorComChamados() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(chamados.existsByCategoriaSetorId(1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> setorService.excluir(1L));
        verify(setores, never()).delete(any(Setor.class));
    }

    @Test
    void deveImpedirExclusaoDeSetorComCategorias() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(categorias.existsBySetorId(1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> setorService.excluir(1L));
        verify(setores, never()).delete(any(Setor.class));
    }

    @Test
    void deveImpedirExclusaoDeSetorComUsuarios() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(usuarios.existsBySetorId(1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> setorService.excluir(1L));
        verify(setores, never()).delete(any(Setor.class));
    }

    @Test
    void deveExcluirSetorSemVinculos() {
        Setor setor = new Setor();
        when(setores.findById(1L)).thenReturn(Optional.of(setor));
        setorService.excluir(1L);
        verify(setores).delete(setor);
    }

    @Test
    void deveImpedirExclusaoDeCategoriaComChamados() {
        when(categorias.findById(1L)).thenReturn(Optional.of(new Categoria()));
        when(chamados.existsByCategoriaId(1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> categoriaService.excluir(1L));
        verify(categorias, never()).delete(any(Categoria.class));
    }

    @Test
    void deveExcluirCategoriaSemChamados() {
        Categoria categoria = new Categoria();
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        categoriaService.excluir(1L);
        verify(categorias).delete(categoria);
    }

    @Test
    void deveRejeitarCategoriaComSetorInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categoriaService.criar(new CategoriaRequest("Rede", "Wi-Fi", 99L)));
        verify(categorias, never()).save(any());
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        when(usuarios.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> usuarioService.criar(solicitante()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deveRejeitarEmailDeOutroUsuarioNaAtualizacao() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(new Usuario()));
        when(usuarios.existsByEmailIgnoreCaseAndIdNot("ana@example.com", 1L)).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> usuarioService.atualizar(1L, solicitante()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deveAceitarEmailDoProprioUsuarioNaAtualizacao() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarios.save(usuario)).thenReturn(usuario);
        assertEquals("ana@example.com", usuarioService.atualizar(1L, solicitante()).email());
        verify(usuarios).existsByEmailIgnoreCaseAndIdNot("ana@example.com", 1L);
    }

    @Test
    void deveRejeitarTecnicoSemSetor() {
        var request = new UsuarioRequest("Ana", "ana@example.com", "123", TipoUsuario.TECNICO, null);
        assertThrows(RegraNegocioException.class, () -> usuarioService.criar(request));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deveRejeitarUsuarioComSetorInexistente() {
        var request = new UsuarioRequest("Ana", "ana@example.com", "123", TipoUsuario.TECNICO, 99L);
        assertThrows(RecursoNaoEncontradoException.class, () -> usuarioService.criar(request));
        verify(usuarios, never()).save(any());
    }

    @Test
    void deveCriarSolicitanteSemSetor() {
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        assertNull(usuarioService.criar(solicitante()).setorId());
        verify(setores, never()).findById(any());
    }

    @Test
    void deveRetornarErroParaCadastrosInexistentes() {
        assertThrows(RecursoNaoEncontradoException.class, () -> setorService.buscar(99L));
        assertThrows(RecursoNaoEncontradoException.class, () -> categoriaService.buscar(99L));
        assertThrows(RecursoNaoEncontradoException.class, () -> usuarioService.buscar(99L));
    }

    private UsuarioRequest solicitante() {
        return new UsuarioRequest("Ana", "ana@example.com", "123", TipoUsuario.SOLICITANTE, null);
    }
}
