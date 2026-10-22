package br.edu.ifpb.curadoria.dto;

public record EnderecoResponse(Long id, String tipoLogradouro, String logradouro, String numero,
        String complemento, String bairro, String cep, String municipio, String uf, String fonteDados) { }
