package br.edu.ifpb.curadoria.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FornecedorRequestTest {

    @Test
    void aceitaFornecedorIdentificadoPorCnpj() {
        FornecedorRequest request = new FornecedorRequest(
                "04.252.011/0001-10",
                null,
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );

        assertTrue(request.isDocumentoValido());
    }

    @Test
    void aceitaFornecedorIdentificadoPorCpf() {
        FornecedorRequest request = new FornecedorRequest(
                null,
                "529.982.247-25",
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );

        assertTrue(request.isDocumentoValido());
    }

    @Test
    void rejeitaCadastroSemDocumento() {
        FornecedorRequest request = new FornecedorRequest(
                null,
                null,
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );

        assertFalse(request.isDocumentoValido());
    }

    @Test
    void rejeitaCadastroComCpfECnpj() {
        FornecedorRequest request = new FornecedorRequest(
                "04.252.011/0001-10",
                "529.982.247-25",
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );

        assertFalse(request.isDocumentoValido());
    }

    @Test
    void rejeitaCadastroComDocumentosEmBranco() {
        FornecedorRequest request = new FornecedorRequest(
                " ",
                " ",
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );

        assertFalse(request.isDocumentoValido());
    }
}