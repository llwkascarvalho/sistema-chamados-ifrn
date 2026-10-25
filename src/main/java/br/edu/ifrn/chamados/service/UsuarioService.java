package br.edu.ifrn.chamados.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.usuario.UsuarioRequest;
import br.edu.ifrn.chamados.dto.usuario.UsuarioResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;

    public Page<UsuarioResponse> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable) {
        return usuarioRepository.listar(nome, tipo, setorId, pageable).map(this::resposta);
    }

    public UsuarioResponse buscar(Long id) {
        return resposta(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraNegocioException("Email ja cadastrado");
        }
        Usuario usuario = new Usuario();
        preencher(usuario, request);
        return resposta(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidade(id);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new RegraNegocioException("Email ja cadastrado");
        }
        preencher(usuario, request);
        return resposta(usuarioRepository.save(usuario));
    }

    private void preencher(Usuario usuario, UsuarioRequest request) {
        if (request.tipo() == TipoUsuario.TECNICO && request.setorId() == null) {
            throw new RegraNegocioException("Tecnico deve pertencer a um setor");
        }
        Setor setor = null;
        if (request.setorId() != null) {
            setor = setorRepository.findById(request.setorId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Setor nao encontrado"));
        }
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setMatricula(request.matricula());
        usuario.setTipo(request.tipo());
        usuario.setSetor(setor);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    private UsuarioResponse resposta(Usuario usuario) {
        Long setorId = usuario.getSetor() == null ? null : usuario.getSetor().getId();
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getMatricula(), usuario.getTipo(), setorId);
    }
}
