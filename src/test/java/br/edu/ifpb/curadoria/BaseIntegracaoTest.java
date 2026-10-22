package br.edu.ifpb.curadoria;

import br.edu.ifpb.curadoria.repository.CnaeRepository;
import br.edu.ifpb.curadoria.repository.EnderecoRepository;
import br.edu.ifpb.curadoria.repository.FornecedorRepository;
import br.edu.ifpb.curadoria.repository.MunicipioRepository;
import br.edu.ifpb.curadoria.repository.NaturezaJuridicaRepository;
import br.edu.ifpb.curadoria.repository.PorteEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base dos testes de integração: sobe o contexto completo com H2 em memória
 * (src/test/resources/application.properties) e limpa as tabelas antes de cada teste.
 */
@SpringBootTest
public abstract class BaseIntegracaoTest {

    @Autowired protected WebApplicationContext contexto;
    @Autowired protected FornecedorRepository fornecedores;
    @Autowired protected EnderecoRepository enderecos;
    @Autowired protected MunicipioRepository municipios;
    @Autowired protected CnaeRepository cnaes;
    @Autowired protected NaturezaJuridicaRepository naturezas;
    @Autowired protected PorteEmpresaRepository portes;

    protected MockMvc mvc;

    @BeforeEach
    public void prepararAmbiente() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        // fornecedores primeiro: a cascata remove os endereços antes dos municípios e catálogos
        fornecedores.deleteAll();
        municipios.deleteAll();
        cnaes.deleteAll();
        naturezas.deleteAll();
        portes.deleteAll();
    }
}
