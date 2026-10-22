package br.edu.ifpb.curadoria.dto;

import java.util.List;

public record FornecedorResponse(Long id, String cnpj, String cpf, String razaoSocial,
        Boolean ativo, Boolean habilitadoLicitar, Integer cnaeCodigo, String cnaeDescricao,
        Integer naturezaJuridicaCodigo, String naturezaJuridicaDescricao,
        Integer porteEmpresaCodigo, String porteEmpresaDescricao,
        List<EnderecoResponse> enderecos) { }
