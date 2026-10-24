package br.edu.ifrn.chamados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.Usuario;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PersistenciaTests {

    private final EntityManager entityManager;

    @Autowired
    PersistenciaTests(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    void devePersistirChamadoComInteracaoESeusRelacionamentos() {
        Usuario solicitante = entityManager.createQuery(
                "select u from Usuario u where u.email = :email", Usuario.class)
                .setParameter("email", "ana.silva@example.com")
                .getSingleResult();
        Categoria categoria = entityManager.createQuery(
                "select c from Categoria c where c.nome = :nome", Categoria.class)
                .setParameter("nome", "Rede e internet")
                .getSingleResult();
        Usuario tecnico = entityManager.createQuery(
                "select u from Usuario u where u.email = :email", Usuario.class)
                .setParameter("email", "carla.lima@example.com")
                .getSingleResult();

        Chamado chamado = new Chamado();
        chamado.setTitulo("Wi-Fi indisponivel");
        chamado.setDescricao("Nao consigo conectar a rede.");
        chamado.setLocal("Laboratorio 1");
        chamado.setPrioridade(Prioridade.MEDIA);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        chamado.setTecnico(tecnico);
        entityManager.persist(chamado);

        Interacao interacao = new Interacao();
        interacao.setTexto("Vou verificar o ponto de acesso.");
        interacao.setDataHora(LocalDateTime.now());
        interacao.setAutor(tecnico);
        interacao.setChamado(chamado);
        entityManager.persist(interacao);
        entityManager.flush();
        entityManager.clear();

        Chamado salvo = entityManager.find(Chamado.class, chamado.getId());
        assertNotNull(salvo.getId());
        assertEquals(StatusChamado.EM_ATENDIMENTO, salvo.getStatus());
        assertEquals(Prioridade.MEDIA, salvo.getPrioridade());
        assertEquals("ana.silva@example.com", salvo.getSolicitante().getEmail());
        assertEquals(salvo.getCategoria().getSetor().getId(), salvo.getTecnico().getSetor().getId());
        assertEquals(1, salvo.getInteracoes().size());
        assertEquals("carla.lima@example.com", salvo.getInteracoes().getFirst().getAutor().getEmail());
    }
}
