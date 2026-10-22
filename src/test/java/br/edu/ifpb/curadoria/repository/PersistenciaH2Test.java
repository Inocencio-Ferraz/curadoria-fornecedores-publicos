package br.edu.ifpb.curadoria.repository;

import br.edu.ifpb.curadoria.BaseIntegracaoTest;
import br.edu.ifpb.curadoria.entity.Endereco;
import br.edu.ifpb.curadoria.entity.Fornecedor;
import br.edu.ifpb.curadoria.entity.Municipio;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Persistência real no H2: restrições de unicidade, integridade e cascata de endereços. */
class PersistenciaH2Test extends BaseIntegracaoTest {

    private static Fornecedor porCnpj(String cnpj) {
        return new Fornecedor(cnpj, null, "Fornecedor " + cnpj, true, false);
    }

    @Test
    void bancoRejeitaCnpjDuplicado() {
        fornecedores.saveAndFlush(porCnpj("04252011000110"));

        assertThrows(DataIntegrityViolationException.class,
                () -> fornecedores.saveAndFlush(porCnpj("04252011000110")));
        assertEquals(1, fornecedores.count());
    }

    @Test
    void bancoRejeitaCpfDuplicado() {
        fornecedores.saveAndFlush(new Fornecedor(null, "52998224725", "Pessoa A", true, false));

        assertThrows(DataIntegrityViolationException.class,
                () -> fornecedores.saveAndFlush(
                        new Fornecedor(null, "52998224725", "Pessoa B", true, false)));
        assertEquals(1, fornecedores.count());
    }

    @Test
    void bancoPermiteVariosRegistrosComCpfNuloEComCnpjNulo() {
        fornecedores.saveAndFlush(porCnpj("04252011000110"));
        fornecedores.saveAndFlush(porCnpj("11222333000181"));
        fornecedores.saveAndFlush(new Fornecedor(null, "52998224725", "Pessoa A", true, false));
        fornecedores.saveAndFlush(new Fornecedor(null, "11144477735", "Pessoa B", true, false));

        assertEquals(4, fornecedores.count());
    }

    @Test
    void bancoRejeitaMunicipioDuplicadoPorNomeEUf() {
        municipios.saveAndFlush(new Municipio("JOAO PESSOA", "PB"));

        assertThrows(DataIntegrityViolationException.class,
                () -> municipios.saveAndFlush(new Municipio("JOAO PESSOA", "PB")));

        municipios.saveAndFlush(new Municipio("JOAO PESSOA", "SP"));
        assertEquals(2, municipios.count());
        assertEquals("JOAO PESSOA", municipios.findByNomeAndUf("JOAO PESSOA", "SP").orElseThrow().getNome());
    }

    @Test
    void enderecoPersisteComMunicipioEFornecedor() {
        Municipio municipio = municipios.save(new Municipio("JOAO PESSOA", "PB"));

        Fornecedor fornecedor = porCnpj("04252011000110");
        Endereco endereco = new Endereco(fornecedor);
        endereco.setLogradouro("das Flores");
        endereco.setCep("58000123");
        endereco.setMunicipio(municipio);
        fornecedor.adicionarEndereco(endereco);

        Long id = fornecedores.save(fornecedor).getId();

        assertEquals(1, enderecos.findByFornecedorId(id).size());
        assertEquals(1, enderecos.findByMunicipioId(municipio.getId()).size());
    }

    @Test
    void exclusaoDoFornecedorRemoveSeusEnderecosEMantemMunicipio() {
        Municipio municipio = municipios.save(new Municipio("JOAO PESSOA", "PB"));

        Fornecedor fornecedor = porCnpj("04252011000110");
        Endereco endereco = new Endereco(fornecedor);
        endereco.setMunicipio(municipio);
        fornecedor.adicionarEndereco(endereco);

        Long id = fornecedores.save(fornecedor).getId();
        assertEquals(1, enderecos.count());

        fornecedores.deleteById(id);

        assertEquals(0, enderecos.count());
        assertEquals(1, municipios.count());
    }
}
