package br.edu.ifpb.curadoria.dto;

import br.edu.ifpb.curadoria.validation.Cnpj;
import br.edu.ifpb.curadoria.validation.Cpf;
import br.edu.ifpb.curadoria.validation.GrupoAtualizacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record FornecedorRequest(
        @Cnpj String cnpj,
        @Cpf String cpf,
        @NotBlank @Size(max = 250) String razaoSocial,
        @NotNull(groups = GrupoAtualizacao.class, message = "é obrigatório na atualização") Boolean ativo,
        @NotNull(groups = GrupoAtualizacao.class, message = "é obrigatório na atualização") Boolean habilitadoLicitar,
        Integer cnaeCodigo,
        Integer naturezaJuridicaCodigo,
        Integer porteEmpresaCodigo,
        @Valid @Size(max = 20) List<EnderecoRequest> enderecos) {

    @AssertTrue(message = "Informe apenas um documento: CPF ou CNPJ")
    public boolean isDocumentoValido() {
        boolean temCnpj = cnpj != null && !cnpj.isBlank();
        boolean temCpf = cpf != null && !cpf.isBlank();

        return temCnpj != temCpf;
    }
}
