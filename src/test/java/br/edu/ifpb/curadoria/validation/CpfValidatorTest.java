package br.edu.ifpb.curadoria.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void aceitaCpfValidoComMascara() {
        assertTrue(validator.isValid("529.982.247-25", null));
    }

    @Test
    void aceitaCpfValidoSemMascara() {
        assertTrue(validator.isValid("52998224725", null));
    }

    @Test
    void rejeitaDigitoVerificadorInvalido() {
        assertFalse(validator.isValid("529.982.247-24", null));
    }

    @Test
    void rejeitaSequenciaRepetida() {
        assertFalse(validator.isValid("111.111.111-11", null));
    }

    @Test
    void rejeitaQuantidadeIncorretaDeDigitos() {
        assertFalse(validator.isValid("1234567890", null));
    }

    @Test
    void aceitaNuloParaValidacaoDeObrigatoriedadeSeparada() {
        assertTrue(validator.isValid(null, null));
    }
}