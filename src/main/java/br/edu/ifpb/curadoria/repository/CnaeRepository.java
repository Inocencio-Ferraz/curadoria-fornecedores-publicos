package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.entity.Cnae;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CnaeRepository
        extends JpaRepository<Cnae, Integer> {
}