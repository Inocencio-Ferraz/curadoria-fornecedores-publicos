package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.entity.PorteEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PorteEmpresaRepository
        extends JpaRepository<PorteEmpresa, Integer> {
}