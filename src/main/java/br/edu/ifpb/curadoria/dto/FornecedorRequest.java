package br.edu.ifpb.curadoria.dto;

import br.edu.ifpb.curadoria.validation.Cnpj;
import br.edu.ifpb.curadoria.validation.Cpf;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @Cnpj String cnpj,
        @Cpf String cpf,
        @NotBlank @Size(max = 250) String razaoSocial,
        Boolean ativo,
        Boolean habilitadoLicitar,
        Integer cnaeCodigo,
        Integer naturezaJuridicaCodigo,
        Integer porteEmpresaCodigo) {

    @AssertTrue(message = "Informe apenas um documento: CPF ou CNPJ")
    public boolean isDocumentoValido() {
        boolean temCnpj = cnpj != null && !cnpj.isBlank();
        boolean temCpf = cpf != null && !cpf.isBlank();

        return temCnpj != temCpf;
    }
}