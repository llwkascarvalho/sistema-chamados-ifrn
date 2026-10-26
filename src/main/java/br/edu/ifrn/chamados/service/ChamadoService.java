package br.edu.ifrn.chamados.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.chamado.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoFiltro;
import br.edu.ifrn.chamados.dto.chamado.ChamadoRequest;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResumoResponse;
import br.edu.ifrn.chamados.dto.chamado.ChamadoResponse;
import br.edu.ifrn.chamados.dto.chamado.InteracaoRequest;
import br.edu.ifrn.chamados.dto.chamado.InteracaoResponse;
import br.edu.ifrn.chamados.dto.chamado.ResolverChamadoRequest;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
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
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final InteracaoRepository interacaoRepository;

    public Page<ChamadoResumoResponse> listar(ChamadoFiltro filtro, Pageable pageable) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new DadosInvalidosException("Data inicial deve ser anterior ou igual a data final");
        }
        return chamadoRepository.listar(filtro.status(), filtro.prioridade(), filtro.setorId(),
                filtro.categoriaId(), filtro.solicitanteId(), filtro.tecnicoId(),
                filtro.dataInicio(), filtro.dataFim(), pageable).map(this::resumo);
    }

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

    @Transactional
    public InteracaoResponse registrarInteracao(Long id, InteracaoRequest request) {
        Chamado chamado = buscarEntidade(id);
        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.CANCELADO) {
            throw new RegraNegocioException("Chamado encerrado nao aceita novas interacoes");
        }
        Usuario autor = buscarUsuario(request.autorId());
        boolean solicitante = autor.getId().equals(chamado.getSolicitante().getId());
        boolean tecnico = chamado.getTecnico() != null && autor.getId().equals(chamado.getTecnico().getId());
        if (!solicitante && !tecnico) {
            throw new RegraNegocioException("Apenas o solicitante e o tecnico atribuido podem registrar interacoes");
        }
        Interacao interacao = new Interacao();
        interacao.setTexto(request.texto());
        interacao.setDataHora(LocalDateTime.now());
        interacao.setChamado(chamado);
        interacao.setAutor(autor);
        chamado.getInteracoes().add(interacao);
        return respostaInteracao(interacaoRepository.save(interacao));
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

    private ChamadoResumoResponse resumo(Chamado chamado) {
        Long tecnicoId = chamado.getTecnico() == null ? null : chamado.getTecnico().getId();
        return new ChamadoResumoResponse(chamado.getId(), chamado.getTitulo(), chamado.getLocal(),
                chamado.getPrioridade(), chamado.getStatus(), chamado.getDataAbertura(), chamado.getDataFechamento(),
                chamado.getSolicitante().getId(), chamado.getCategoria().getId(), tecnicoId);
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
