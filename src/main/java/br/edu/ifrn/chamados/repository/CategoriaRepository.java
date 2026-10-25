package br.edu.ifrn.chamados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.ifrn.chamados.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("""
            select c from Categoria c
            where (:nome is null or lower(c.nome) like lower(concat('%', :nome, '%')))
              and (:setorId is null or c.setor.id = :setorId)
            """)
    Page<Categoria> listar(String nome, Long setorId, Pageable pageable);

    boolean existsBySetorId(Long setorId);
}
