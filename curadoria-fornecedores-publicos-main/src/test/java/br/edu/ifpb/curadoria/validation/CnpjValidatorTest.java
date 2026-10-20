package br.edu.ifpb.curadoria.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CnpjValidatorTest {
    private final CnpjValidator validator = new CnpjValidator();
    @Test void aceitaCnpjValidoComMascara() { assertTrue(validator.isValid("04.252.011/0001-10", null)); }
    @Test void rejeitaDigitoVerificadorInvalido() { assertFalse(validator.isValid("04.252.011/0001-11", null)); }
    @Test void rejeitaSequenciaRepetida() { assertFalse(validator.isValid("11.111.111/1111-11", null)); }
    @Test void aceitaNuloParaValidacaoDeObrigatoriedadeSeparada() { assertTrue(validator.isValid(null, null)); }
}
