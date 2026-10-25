package br.edu.ifrn.chamados.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResponse;
import br.edu.ifrn.chamados.dto.chamado.InteracaoResponse;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public ChamadoResponse abrir(ChamadoRequest request) {
        Usuario solicitante = buscarUsuario(request.solicitanteId());
        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE) {
            throw new RegraNegocioException("Apenas solicitantes podem abrir chamados");
        }
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria nao encontrada"));
        Chamado chamado = new Chamado();
        chamado.setTitulo(request.titulo());
        chamado.setDescricao(request.descricao());
        chamado.setLocal(request.local());
        chamado.setPrioridade(request.prioridade() == null ? Prioridade.MEDIA : request.prioridade());
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        return resposta(chamadoRepository.save(chamado));
    }

    public ChamadoResponse buscar(Long id) {
        return resposta(buscarEntidade(id));
    }

    @Transactional
    public ChamadoResponse assumir(Long id, AssumirChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarAberto(chamado);
        Usuario tecnico = buscarUsuario(request.tecnicoId());
        if (tecnico.getTipo() != TipoUsuario.TECNICO || tecnico.getSetor() == null
                || !tecnico.getSetor().getId().equals(chamado.getCategoria().getSetor().getId())) {
            throw new RegraNegocioException("Chamado deve ser assumido por um tecnico do setor responsavel");
        }
        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        return resposta(chamadoRepository.save(chamado));
    }

    @Transactional
    public ChamadoResponse resolver(Long id, ResolverChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        if (chamado.getStatus() != StatusChamado.EM_ATENDIMENTO) {
            throw new RegraNegocioException("Apenas chamados em atendimento podem ser resolvidos");
        }
        Usuario tecnico = buscarUsuario(request.tecnicoId());
        if (tecnico.getTipo() != TipoUsuario.TECNICO || chamado.getTecnico() == null
                || !tecnico.getId().equals(chamado.getTecnico().getId())) {
            throw new RegraNegocioException("Apenas o tecnico atribuido pode resolver o chamado");
        }
        if (request.solucao() == null || request.solucao().isBlank()) {
            throw new RegraNegocioException("Solucao e obrigatoria para resolver o chamado");
        }
        chamado.setSolucao(request.solucao());
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataFechamento(LocalDateTime.now());
        return resposta(chamadoRepository.save(chamado));
    }

    @Transactional
    public ChamadoResponse cancelar(Long id, CancelarChamadoRequest request) {
        Chamado chamado = buscarEntidade(id);
        validarAberto(chamado);
        Usuario solicitante = buscarUsuario(request.solicitanteId());
        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE
                || !solicitante.getId().equals(chamado.getSolicitante().getId())) {
            throw new RegraNegocioException("Apenas o proprio solicitante pode cancelar o chamado");
        }
        chamado.setStatus(StatusChamado.CANCELADO);
        chamado.setDataFechamento(LocalDateTime.now());
        return resposta(chamadoRepository.save(chamado));
    }

    private void validarAberto(Chamado chamado) {
        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new RegraNegocioException("Esta acao exige um chamado aberto");
        }
    }

    private Chamado buscarEntidade(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado nao encontrado"));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    private ChamadoResponse resposta(Chamado chamado) {
        Long tecnicoId = chamado.getTecnico() == null ? null : chamado.getTecnico().getId();
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
                chamado.getLocal(), chamado.getPrioridade(), chamado.getStatus(), chamado.getDataAbertura(),
                chamado.getDataFechamento(), chamado.getSolucao(), chamado.getSolicitante().getId(),
                chamado.getCategoria().getId(), tecnicoId,
                chamado.getInteracoes().stream().map(this::respostaInteracao).toList());
    }

    private InteracaoResponse respostaInteracao(Interacao interacao) {
        return new InteracaoResponse(interacao.getId(), interacao.getTexto(), interacao.getDataHora(),
                interacao.getAutor().getId());
    }
}
