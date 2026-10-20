package br.edu.ifpb.curadoria.repository;
import br.edu.ifpb.curadoria.entity.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Optional<Municipio> findByNomeAndUf(String nome, String uf);
}