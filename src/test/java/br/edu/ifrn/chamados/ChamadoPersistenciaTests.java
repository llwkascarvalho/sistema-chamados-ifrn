package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
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
class ChamadoPersistenciaTests {

    private final ChamadoService chamados;
    private final SetorService setores;
    private final CategoriaService categorias;
    private final UsuarioService usuarios;
    private final EntityManager entityManager;

    private Long solicitanteId;
    private Long tecnicoId;
    private Long categoriaId;

    @Autowired
    ChamadoPersistenciaTests(ChamadoService chamados, SetorService setores, CategoriaService categorias,
            UsuarioService usuarios, EntityManager entityManager) {
        this.chamados = chamados;
        this.setores = setores;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void prepararCadastros() {
        var setor = setores.criar(new SetorRequest("Setor de suporte teste", "TEST-FLUXO"));
        categoriaId = categorias.criar(new CategoriaRequest("Rede teste", "Wi-Fi", setor.id())).id();
        solicitanteId = usuarios.criar(new UsuarioRequest("Solicitante teste", "fluxo.solicitante@example.com",
                "20260101", TipoUsuario.SOLICITANTE, null)).id();
        tecnicoId = usuarios.criar(new UsuarioRequest("Tecnico teste", "fluxo.tecnico@example.com",
                "20260102", TipoUsuario.TECNICO, setor.id())).id();
    }

    @Test
    void devePersistirFluxoCompletoComHistoricoOrdenado() {
        var aberto = chamados.abrir(abertura());
        assertEquals(StatusChamado.ABERTO, aberto.status());
        assertEquals(Prioridade.MEDIA, aberto.prioridade());
        assertNull(aberto.tecnicoId());
        chamados.registrarInteracao(aberto.id(), new InteracaoRequest("Mais detalhes do problema", solicitanteId));
        chamados.assumir(aberto.id(), new AssumirChamadoRequest(tecnicoId));
        chamados.registrarInteracao(aberto.id(), new InteracaoRequest("Verificando o equipamento", tecnicoId));
        chamados.resolver(aberto.id(), new ResolverChamadoRequest(tecnicoId, "Cabo substituido"));
        entityManager.flush();
        entityManager.clear();

        var salvo = chamados.buscar(aberto.id());
        assertEquals(StatusChamado.RESOLVIDO, salvo.status());
        assertEquals(tecnicoId, salvo.tecnicoId());
        assertEquals("Cabo substituido", salvo.solucao());
        assertNotNull(salvo.dataAbertura());
        assertNotNull(salvo.dataFechamento());
        assertEquals(List.of("Mais detalhes do problema", "Verificando o equipamento"),
                salvo.interacoes().stream().map(i -> i.texto()).toList());
        assertEquals(List.of(solicitanteId, tecnicoId), salvo.interacoes().stream().map(i -> i.autorId()).toList());
        verificarBloqueioAposEncerramento(salvo.id());
    }

    @Test
    void devePersistirCancelamentoEBloquearNovasAcoes() {
        var aberto = chamados.abrir(abertura());
        chamados.cancelar(aberto.id(), new CancelarChamadoRequest(solicitanteId));
        entityManager.flush();
        entityManager.clear();

        var salvo = chamados.buscar(aberto.id());
        assertEquals(StatusChamado.CANCELADO, salvo.status());
        assertNotNull(salvo.dataFechamento());
        assertNull(salvo.tecnicoId());
        assertEquals(0, salvo.interacoes().size());
        verificarBloqueioAposEncerramento(salvo.id());
    }

    @Test
    void deveManterChamadoAbertoAoRejeitarTecnicoDeOutroSetorETerceiros() {
        var outroSetor = setores.criar(new SetorRequest("Outro setor de teste", "TEST-OUTRO"));
        var outroTecnico = usuarios.criar(new UsuarioRequest("Outro tecnico", "fluxo.outro@example.com",
                "20260103", TipoUsuario.TECNICO, outroSetor.id()));
        var outroSolicitante = usuarios.criar(new UsuarioRequest("Outro solicitante", "fluxo.terceiro@example.com",
                "20260104", TipoUsuario.SOLICITANTE, null));
        var aberto = chamados.abrir(abertura());

        assertThrows(RegraNegocioException.class,
                () -> chamados.assumir(aberto.id(), new AssumirChamadoRequest(outroTecnico.id())));
        assertThrows(RegraNegocioException.class,
                () -> chamados.cancelar(aberto.id(), new CancelarChamadoRequest(outroSolicitante.id())));
        assertThrows(RegraNegocioException.class,
                () -> chamados.registrarInteracao(aberto.id(), new InteracaoRequest("Teste", outroTecnico.id())));
        entityManager.flush();
        entityManager.clear();

        var salvo = chamados.buscar(aberto.id());
        assertEquals(StatusChamado.ABERTO, salvo.status());
        assertNull(salvo.tecnicoId());
        assertNull(salvo.dataFechamento());
        assertEquals(0, salvo.interacoes().size());
    }

    private ChamadoRequest abertura() {
        return new ChamadoRequest("Sem rede", "Wi-Fi indisponivel", "Laboratorio 1", null,
                solicitanteId, categoriaId);
    }

    private void verificarBloqueioAposEncerramento(Long id) {
        assertThrows(RegraNegocioException.class, () -> chamados.assumir(id, new AssumirChamadoRequest(tecnicoId)));
        assertThrows(RegraNegocioException.class,
                () -> chamados.resolver(id, new ResolverChamadoRequest(tecnicoId, "Outra solucao")));
        assertThrows(RegraNegocioException.class, () -> chamados.cancelar(id, new CancelarChamadoRequest(solicitanteId)));
        assertThrows(RegraNegocioException.class,
                () -> chamados.registrarInteracao(id, new InteracaoRequest("Nova interacao", solicitanteId)));
    }
}
