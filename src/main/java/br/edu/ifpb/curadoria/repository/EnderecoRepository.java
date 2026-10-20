package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnderecoRepository
        extends JpaRepository<Endereco, Long> {

    List<Endereco> findByFornecedorId(Long fornecedorId);

    List<Endereco> findByMunicipioId(Long municipioId);
}