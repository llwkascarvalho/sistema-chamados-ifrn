package br.edu.ifrn.chamados.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifrn.chamados.dto.categoria.CategoriaRequest;
import br.edu.ifrn.chamados.dto.categoria.CategoriaResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SetorRepository setorRepository;
    private final ChamadoRepository chamadoRepository;

    public Page<CategoriaResponse> listar(String nome, Long setorId, Pageable pageable) {
        return categoriaRepository.listar(nome, setorId, pageable).map(this::resposta);
    }

    public Page<CategoriaResponse> listarPorSetor(Long setorId, String nome, Pageable pageable) {
        buscarSetor(setorId);
        return listar(nome, setorId, pageable);
    }

    public CategoriaResponse buscar(Long id) {
        return resposta(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        preencher(categoria, request);
        return resposta(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidade(id);
        preencher(categoria, request);
        return resposta(categoriaRepository.save(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Categoria possui chamados vinculados");
        }
        categoriaRepository.delete(categoria);
    }

    private void preencher(Categoria categoria, CategoriaRequest request) {
        Setor setor = buscarSetor(request.setorId());
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        categoria.setSetor(setor);
    }

    private Setor buscarSetor(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor nao encontrado"));
    }

    private Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria nao encontrada"));
    }

    private CategoriaResponse resposta(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao(),
                categoria.getSetor().getId());
    }
}
