package br.edu.ifpb.curadoria.transform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DadosNormalizerTest {
    @Test void normalizaDocumentoETextoSemInventarValores() {
        assertEquals("04252011000110", DadosNormalizer.documento("04.252.011/0001-10"));
        assertEquals("Fornecedor Público", DadosNormalizer.texto("  Fornecedor   Público  "));
        assertNull(DadosNormalizer.texto("   "));
    }
    @Test void padronizaUf() { assertEquals("PB", DadosNormalizer.uf(" pb ")); }
    @Test void normalizaMunicipioSemAcentoEmMaiusculas() {
        assertEquals("JOAO PESSOA", DadosNormalizer.municipio("  João   Pessoa "));
        assertEquals("SAO LUIS", DadosNormalizer.municipio("são luís"));
        assertNull(DadosNormalizer.municipio("   "));
        assertNull(DadosNormalizer.municipio(null));
    }
    @Test void normalizaCepSomenteDigitos() {
        assertEquals("58000123", DadosNormalizer.cep("58000-123"));
        assertNull(DadosNormalizer.cep("sem numeros"));
        assertNull(DadosNormalizer.cep(null));
    }
}
