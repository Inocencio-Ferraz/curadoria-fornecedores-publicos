package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.entity.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    Optional<Fornecedor> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    Page<Fornecedor> findByRazaoSocialContainingIgnoreCase(String razaoSocial, Pageable pageable);
    Page<Fornecedor> findByAtivo(Boolean ativo, Pageable pageable);
    Page<Fornecedor> findByCnaeCodigo(Integer codigo, Pageable pageable);
    Page<Fornecedor> findByPorteEmpresaCodigo(Integer codigo, Pageable pageable);
}
