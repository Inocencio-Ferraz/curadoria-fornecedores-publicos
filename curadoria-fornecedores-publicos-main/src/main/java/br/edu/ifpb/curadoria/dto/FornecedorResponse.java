package br.edu.ifpb.curadoria.dto;

public record FornecedorResponse(Long id, String cnpj, String cpf, String razaoSocial,
        Boolean ativo, Boolean habilitadoLicitar, Integer cnaeCodigo, String cnaeDescricao,
        Integer naturezaJuridicaCodigo, String naturezaJuridicaDescricao,
        Integer porteEmpresaCodigo, String porteEmpresaDescricao) { }
