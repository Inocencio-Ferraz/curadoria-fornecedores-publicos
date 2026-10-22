package br.edu.ifpb.curadoria.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record EnderecoRequest(
        @Size(max = 50) String tipoLogradouro,
        @Size(max = 250) String logradouro,
        @Size(max = 30) String numero,
        @Size(max = 250) String complemento,
        @Size(max = 150) String bairro,
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep,
        @Size(max = 150) String municipio,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF inválida") String uf) {

    @AssertTrue(message = "Informe município e UF juntos")
    public boolean isMunicipioValido() {
        return preenchido(municipio) == preenchido(uf);
    }

    @AssertTrue(message = "Informe ao menos logradouro, CEP ou município")
    public boolean isConteudoValido() {
        return preenchido(logradouro) || preenchido(cep) || preenchido(municipio);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
