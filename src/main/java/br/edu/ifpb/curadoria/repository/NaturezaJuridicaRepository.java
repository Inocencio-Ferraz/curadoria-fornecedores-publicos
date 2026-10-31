package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.entity.NaturezaJuridica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaturezaJuridicaRepository
        extends JpaRepository<NaturezaJuridica, Integer> {
}