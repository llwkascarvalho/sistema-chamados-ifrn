package br.edu.ifrn.chamados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
            select u from Usuario u
            where (:nome is null or lower(u.nome) like lower(concat('%', :nome, '%')))
              and (:tipo is null or u.tipo = :tipo)
              and (:setorId is null or u.setor.id = :setorId)
            """)
    Page<Usuario> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsBySetorId(Long setorId);
}
