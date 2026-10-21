package br.edu.ifpb.curadoria.dto;

import br.edu.ifpb.curadoria.validation.Cnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank @Cnpj String cnpj,
        @Size(max = 14) String cpf,
        @NotBlank @Size(max = 250) String razaoSocial,
        Boolean ativo,
        Boolean habilitadoLicitar,
        Integer cnaeCodigo,
        Integer naturezaJuridicaCodigo,
        Integer porteEmpresaCodigo) { }
