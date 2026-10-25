package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.SetorService;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class CadastroPersistenciaTests {

    private final SetorService setores;
    private final CategoriaService categorias;
    private final UsuarioService usuarios;
    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EntityManager entityManager;

    @Autowired
    CadastroPersistenciaTests(SetorService setores, CategoriaService categorias, UsuarioService usuarios,
            SetorRepository setorRepository, CategoriaRepository categoriaRepository, EntityManager entityManager) {
        this.setores = setores;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.setorRepository = setorRepository;
        this.categoriaRepository = categoriaRepository;
        this.entityManager = entityManager;
    }

    @Test
    void deveCriarAtualizarConsultarEExcluirCadastros() {
        var setor = setores.criar(new SetorRequest("Setor de teste", "TEST-CAD"));
        var categoria = categorias.criar(new CategoriaRequest("Rede teste", "Wi-Fi", setor.id()));
        var usuario = usuarios.criar(new UsuarioRequest("Ana teste", "cadastro.teste@example.com",
                "123", TipoUsuario.SOLICITANTE, null));

        setores.atualizar(setor.id(), new SetorRequest("Setor atualizado", setor.sigla()));
        categorias.atualizar(categoria.id(), new CategoriaRequest("Rede atualizada", "Internet", setor.id()));
        usuarios.atualizar(usuario.id(), new UsuarioRequest("Ana atualizada", usuario.email(),
                "456", TipoUsuario.SOLICITANTE, null));
        entityManager.flush();
        entityManager.clear();

        assertEquals("Setor atualizado", setores.buscar(setor.id()).nome());
        assertEquals("Internet", categorias.buscar(categoria.id()).descricao());
        assertEquals("456", usuarios.buscar(usuario.id()).matricula());
        assertNull(usuarios.buscar(usuario.id()).setorId());

        categorias.excluir(categoria.id());
        entityManager.flush();
        setores.excluir(setor.id());
        entityManager.flush();
        assertFalse(categoriaRepository.existsById(categoria.id()));
        assertFalse(setorRepository.existsById(setor.id()));
    }

    @Test
    void deveFiltrarSemDiferenciarMaiusculasECombinarParametros() {
        var setor = setores.criar(new SetorRequest("Setor de teste", "TEST-CAD"));
        categorias.criar(new CategoriaRequest("Rede de teste", "Wi-Fi", setor.id()));
        usuarios.criar(new UsuarioRequest("Ana teste", "cadastro.teste@example.com",
                "123", TipoUsuario.TECNICO, setor.id()));
        var pagina = PageRequest.of(0, 10, Sort.by("nome"));

        assertEquals(1, setores.listar("sETor de TESTE", pagina).getTotalElements());
        assertEquals(1, categorias.listar("rEDe", setor.id(), pagina).getTotalElements());
        assertEquals(0, categorias.listar("Eletrica", setor.id(), pagina).getTotalElements());
        assertEquals(1, categorias.listarPorSetor(setor.id(), null, pagina).getTotalElements());
        assertEquals(1, usuarios.listar("aNA teSTE", TipoUsuario.TECNICO, setor.id(), pagina).getTotalElements());
        assertEquals(0, usuarios.listar(null, TipoUsuario.SOLICITANTE, setor.id(), pagina).getTotalElements());
        assertTrue(categorias.listar(null, null, pagina).getTotalElements() >= 5);
    }

    @Test
    void deveListarSolicitanteSemSetorQuandoFiltrosForemOmitidos() {
        var usuario = usuarios.criar(new UsuarioRequest("Solicitante teste", "cadastro.teste@example.com",
                "123", TipoUsuario.SOLICITANTE, null));
        var pagina = PageRequest.of(0, 50);
        assertTrue(usuarios.listar(null, null, null, pagina).stream().anyMatch(u -> u.id().equals(usuario.id())));
        assertEquals(1, usuarios.listar("SOLICITANTE TESTE", null, null, pagina).getTotalElements());
    }

    @Test
    void deveRejeitarDuplicidadesSemDiferenciarMaiusculas() {
        setores.criar(new SetorRequest("Setor de teste", "TEST-CAD"));
        usuarios.criar(new UsuarioRequest("Ana teste", "cadastro.teste@example.com",
                "123", TipoUsuario.SOLICITANTE, null));
        assertThrows(RegraNegocioException.class,
                () -> setores.criar(new SetorRequest("Outro setor", "test-cad")));
        assertThrows(RegraNegocioException.class, () -> usuarios.criar(new UsuarioRequest(
                "Outra pessoa", "CADASTRO.TESTE@example.com", "456", TipoUsuario.SOLICITANTE, null)));
    }

    @ParameterizedTest
    @EnumSource(StatusChamado.class)
    void deveImpedirExclusaoComChamadoEmQualquerStatus(StatusChamado status) {
        var setor = setores.criar(new SetorRequest("Setor de teste", "TEST-CAD"));
        var categoria = categorias.criar(new CategoriaRequest("Rede teste", "Wi-Fi", setor.id()));
        var usuario = usuarios.criar(new UsuarioRequest("Ana teste", "cadastro.teste@example.com",
                "123", TipoUsuario.SOLICITANTE, null));
        Chamado chamado = new Chamado();
        chamado.setTitulo("Sem conexao");
        chamado.setDescricao("Wi-Fi indisponivel");
        chamado.setLocal("Sala 1");
        chamado.setPrioridade(Prioridade.MEDIA);
        chamado.setStatus(status);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setCategoria(entityManager.getReference(Categoria.class, categoria.id()));
        chamado.setSolicitante(entityManager.getReference(Usuario.class, usuario.id()));
        entityManager.persist(chamado);
        entityManager.flush();

        assertThrows(RegraNegocioException.class, () -> categorias.excluir(categoria.id()));
        assertThrows(RegraNegocioException.class, () -> setores.excluir(setor.id()));
    }
}
