package br.edu.ifpb.curadoria.dto;

import br.edu.ifpb.curadoria.validation.GrupoAtualizacao;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Valida as anotações dos DTOs com o Bean Validation real (sem Spring e sem mocks). */
class ValidacaoRequestTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void encerrar() {
        fabrica.close();
    }

    private static Set<String> propriedades(Set<? extends ConstraintViolation<?>> violacoes) {
        return violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static FornecedorRequest fornecedor(
            String cnpj, String cpf, String razao, Boolean ativo, Boolean habilitado,
            List<EnderecoRequest> enderecos) {
        return new FornecedorRequest(cnpj, cpf, razao, ativo, habilitado, null, null, null, enderecos);
    }

    @Test
    void aceitaFornecedorValidoNoCadastroSemAtivoEHabilitado() {
        var violacoes = validator.validate(
                fornecedor("04.252.011/0001-10", null, "Fornecedor Teste", null, null, null));

        assertTrue(violacoes.isEmpty());
    }

    @Test
    void aceitaCnpjAlfanumericoValido() {
        var violacoes = validator.validate(
                fornecedor("12.ABC.345/01DE-35", null, "Fornecedor Teste", null, null, null));

        assertTrue(violacoes.isEmpty());
    }

    @Test
    void rejeitaCnpjECpfInvalidos() {
        assertTrue(propriedades(validator.validate(
                fornecedor("04.252.011/0001-11", null, "X", null, null, null))).contains("cnpj"));
        assertTrue(propriedades(validator.validate(
                fornecedor(null, "529.982.247-24", "X", null, null, null))).contains("cpf"));
    }

    @Test
    void rejeitaDocumentosAusentesOuSimultaneos() {
        assertTrue(propriedades(validator.validate(
                fornecedor(null, null, "X", null, null, null))).contains("documentoValido"));
        assertTrue(propriedades(validator.validate(
                fornecedor("04.252.011/0001-10", "529.982.247-25", "X", null, null, null)))
                .contains("documentoValido"));
    }

    @Test
    void rejeitaRazaoSocialVaziaOuLonga() {
        assertTrue(propriedades(validator.validate(
                fornecedor("04.252.011/0001-10", null, "   ", null, null, null))).contains("razaoSocial"));
        assertTrue(propriedades(validator.validate(
                fornecedor("04.252.011/0001-10", null, "A".repeat(251), null, null, null)))
                .contains("razaoSocial"));
    }

    @Test
    void atualizacaoExigeAtivoEHabilitadoLicitar() {
        FornecedorRequest incompleto =
                fornecedor("04.252.011/0001-10", null, "Fornecedor Teste", null, null, null);

        assertTrue(validator.validate(incompleto).isEmpty());

        var violacoes = propriedades(validator.validate(incompleto, GrupoAtualizacao.class));
        assertTrue(violacoes.contains("ativo"));
        assertTrue(violacoes.contains("habilitadoLicitar"));

        FornecedorRequest completo =
                fornecedor("04.252.011/0001-10", null, "Fornecedor Teste", true, false, null);
        assertTrue(validator.validate(completo, GrupoAtualizacao.class).isEmpty());
    }

    @Test
    void validaEnderecosAninhados() {
        EnderecoRequest invalido = new EnderecoRequest(
                null, "Rua A", null, null, null, "123", "João Pessoa", null);

        var violacoes = propriedades(validator.validate(
                fornecedor("04.252.011/0001-10", null, "Fornecedor Teste", null, null, List.of(invalido))));

        assertTrue(violacoes.contains("enderecos[0].cep"));
        assertTrue(violacoes.contains("enderecos[0].municipioValido"));
    }

    @Test
    void enderecoExigeUfValidaEConteudoMinimo() {
        EnderecoRequest ufInvalida = new EnderecoRequest(
                null, "Rua A", null, null, null, null, "João Pessoa", "PBX");
        assertTrue(propriedades(validator.validate(ufInvalida)).contains("uf"));

        EnderecoRequest vazio = new EnderecoRequest(
                null, " ", null, null, "Centro", null, null, null);
        assertTrue(propriedades(validator.validate(vazio)).contains("conteudoValido"));

        EnderecoRequest valido = new EnderecoRequest(
                "Rua", "das Flores", "10", null, "Centro", "58000-123", "João Pessoa", "PB");
        assertTrue(validator.validate(valido).isEmpty());
    }

    @Test
    void limitaQuantidadeDeEnderecos() {
        EnderecoRequest endereco = new EnderecoRequest(
                null, "Rua A", null, null, null, null, null, null);

        var violacoes = propriedades(validator.validate(
                fornecedor("04.252.011/0001-10", null, "Fornecedor Teste", null, null,
                        java.util.Collections.nCopies(21, endereco))));

        assertTrue(violacoes.contains("enderecos"));
    }
}
