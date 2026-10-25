package br.edu.ifrn.chamados.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.setor.SetorRequest;
import br.edu.ifrn.chamados.dto.setor.SetorResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetorService {

    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;

    public Page<SetorResponse> listar(String nome, Pageable pageable) {
        return setorRepository.findByNomeContainingIgnoreCase(nome == null ? "" : nome, pageable)
                .map(this::resposta);
    }

    public SetorResponse buscar(Long id) {
        return resposta(buscarEntidade(id));
    }

    @Transactional
    public SetorResponse criar(SetorRequest request) {
        if (setorRepository.existsBySiglaIgnoreCase(request.sigla())) {
            throw new RegraNegocioException("Sigla ja cadastrada");
        }
        Setor setor = new Setor();
        setor.setNome(request.nome());
        setor.setSigla(request.sigla());
        return resposta(setorRepository.save(setor));
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest request) {
        Setor setor = buscarEntidade(id);
        if (setorRepository.existsBySiglaIgnoreCaseAndIdNot(request.sigla(), id)) {
            throw new RegraNegocioException("Sigla ja cadastrada");
        }
        setor.setNome(request.nome());
        setor.setSigla(request.sigla());
        return resposta(setorRepository.save(setor));
    }

    @Transactional
    public void excluir(Long id) {
        Setor setor = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaSetorId(id)) {
            throw new RegraNegocioException("Setor possui chamados vinculados");
        }
        if (categoriaRepository.existsBySetorId(id) || usuarioRepository.existsBySetorId(id)) {
            throw new RegraNegocioException("Setor possui categorias ou usuarios vinculados");
        }
        setorRepository.delete(setor);
    }

    private Setor buscarEntidade(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor nao encontrado"));
    }

    private SetorResponse resposta(Setor setor) {
        return new SetorResponse(setor.getId(), setor.getNome(), setor.getSigla());
    }
}
