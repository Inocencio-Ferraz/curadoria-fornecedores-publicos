package br.edu.ifpb.curadoria.controller;

import br.edu.ifpb.curadoria.BaseIntegracaoTest;
import br.edu.ifpb.curadoria.entity.Cnae;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contrato HTTP completo: controller + validação + serviço + JPA + H2 em memória. */
class FornecedorApiIntegrationTest extends BaseIntegracaoTest {

    // Documentos sintéticos válidos (dígitos verificadores conferidos), sem relação com pessoas reais
    private static final String CNPJ_1 = "04252011000110";
    private static final String CNPJ_2 = "11222333000181";
    private static final String CNPJ_ALFA = "12ABC34501DE35";
    private static final String CPF_1 = "52998224725";

    // ------------------------------------------------------------ helpers

    private static String json(String cnpj, String cpf, String razao, String ativo, String habilitado, String extra) {
        StringBuilder sb = new StringBuilder("{");
        if (cnpj != null) sb.append("\"cnpj\":\"").append(cnpj).append("\",");
        if (cpf != null) sb.append("\"cpf\":\"").append(cpf).append("\",");
        sb.append("\"razaoSocial\":\"").append(razao).append("\"");
        if (ativo != null) sb.append(",\"ativo\":").append(ativo);
        if (habilitado != null) sb.append(",\"habilitadoLicitar\":").append(habilitado);
        if (extra != null) sb.append(",").append(extra);
        return sb.append("}").toString();
    }

    private static String cnpjJson(String cnpj, String razao) {
        return json(cnpj, null, razao, null, null, null);
    }

    private ResultActions enviarPost(String corpo) throws Exception {
        return mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions enviarPut(long id, String corpo) throws Exception {
        return mvc.perform(put("/api/fornecedores/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long criar(String corpo) throws Exception {
        String resposta = enviarPost(corpo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(resposta);
        assertTrue(m.find(), "resposta sem id: " + resposta);
        return Long.parseLong(m.group(1));
    }

    /** Verifica que o corpo da resposta contém o trecho (sem depender de Hamcrest). */
    private static ResultMatcher corpoContem(String trecho) {
        return resultado -> {
            String corpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertTrue(corpo.contains(trecho), "esperava '" + trecho + "' em: " + corpo);
        };
    }

    private static final String ENDERECO_JP = """
            "enderecos":[{"tipoLogradouro":"Rua","logradouro":"das Flores","numero":"10",
            "bairro":"Centro","cep":"58000-123","municipio":"João Pessoa","uf":"pb"}]""";

    // ------------------------------------------------- cadastro e consulta

    @Test
    void cadastraComCnpjMascaradoERetorna201() throws Exception {
        enviarPost(cnpjJson("04.252.011/0001-10", "Fornecedor Teste"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cnpj").value(CNPJ_1))
                .andExpect(jsonPath("$.cpf").isEmpty())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.habilitadoLicitar").value(false))
                .andExpect(jsonPath("$.enderecos.length()").value(0));
    }

    @Test
    void cadastraEConsultaPorCpfComMascara() throws Exception {
        enviarPost(json(null, "529.982.247-25", "Pessoa Teste", null, null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value(CPF_1));

        mvc.perform(get("/api/fornecedores/cpf/529.982.247-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Pessoa Teste"));
    }

    @Test
    void consultaPorCnpjIncluindoAlfanumerico() throws Exception {
        criar(cnpjJson(CNPJ_1, "Numerico"));
        criar(cnpjJson("12.ABC.345/01DE-35", "Alfanumerico"));

        mvc.perform(get("/api/fornecedores/cnpj/" + CNPJ_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Numerico"));
        mvc.perform(get("/api/fornecedores/cnpj/" + CNPJ_ALFA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Alfanumerico"));
    }

    @Test
    void consultaPorIdECodigosInexistentesRetornam404() throws Exception {
        mvc.perform(get("/api/fornecedores/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/fornecedores/cnpj/" + CNPJ_1)).andExpect(status().isNotFound());
        mvc.perform(get("/api/fornecedores/cpf/" + CPF_1)).andExpect(status().isNotFound());
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/api/fornecedores/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Requisição inválida"));
    }

    // ----------------------------------------------------- validações HTTP

    @Test
    void rejeitaCnpjInvalido() throws Exception {
        enviarPost(cnpjJson("04.252.011/0001-11", "Fornecedor Teste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").isArray())
                .andExpect(corpoContem("cnpj"));
        assertEquals(0, fornecedores.count());
    }

    @Test
    void rejeitaCpfInvalido() throws Exception {
        enviarPost(json(null, "529.982.247-24", "Pessoa Teste", null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(corpoContem("cpf"));
        assertEquals(0, fornecedores.count());
    }

    @Test
    void rejeitaCpfECnpjSimultaneosOuAusentes() throws Exception {
        enviarPost(json(CNPJ_1, CPF_1, "Fornecedor Teste", null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(corpoContem("Informe apenas um documento"));
        enviarPost(json(null, null, "Fornecedor Teste", null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(corpoContem("Informe apenas um documento"));
        assertEquals(0, fornecedores.count());
    }

    @Test
    void rejeitaRazaoSocialVaziaOuMuitoLonga() throws Exception {
        enviarPost(cnpjJson(CNPJ_1, "   ")).andExpect(status().isBadRequest()).andExpect(corpoContem("razaoSocial"));
        enviarPost(cnpjJson(CNPJ_1, "A".repeat(251))).andExpect(status().isBadRequest()).andExpect(corpoContem("razaoSocial"));
        assertEquals(0, fornecedores.count());
    }

    @Test
    void rejeitaJsonMalformadoOuAusente() throws Exception {
        enviarPost("{").andExpect(status().isBadRequest()).andExpect(corpoContem("malformado"));
        enviarPost("").andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------- duplicidade

    @Test
    void rejeitaCnpjECpfDuplicadosCom409() throws Exception {
        criar(cnpjJson(CNPJ_1, "Primeiro"));
        enviarPost(cnpjJson("04.252.011/0001-10", "Segundo")).andExpect(status().isConflict());

        criar(json(null, CPF_1, "Pessoa", null, null, null));
        enviarPost(json(null, "529.982.247-25", "Outra", null, null, null)).andExpect(status().isConflict());

        assertEquals(2, fornecedores.count());
    }

    // -------------------------------------------------------- relacionamentos

    @Test
    void associaCnaeExistenteEIndicaReferenciaInexistenteCom422() throws Exception {
        cnaes.save(new Cnae(4711301, "Comercio varejista"));

        enviarPost(json(CNPJ_1, null, "Com CNAE", null, null, "\"cnaeCodigo\":4711301"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnaeCodigo").value(4711301))
                .andExpect(jsonPath("$.cnaeDescricao").value("Comercio varejista"));

        enviarPost(json(CNPJ_2, null, "CNAE ruim", null, null, "\"cnaeCodigo\":1")).andExpect(status().is(422));
        enviarPost(json(CNPJ_2, null, "Natureza ruim", null, null, "\"naturezaJuridicaCodigo\":1")).andExpect(status().is(422));
        enviarPost(json(CNPJ_2, null, "Porte ruim", null, null, "\"porteEmpresaCodigo\":1")).andExpect(status().is(422));

        assertEquals(1, fornecedores.count());
    }

    // --------------------------------------------------------- atualização

    @Test
    void putExigeAtivoEHabilitadoLicitar() throws Exception {
        long id = criar(cnpjJson(CNPJ_1, "Original"));

        enviarPut(id, cnpjJson(CNPJ_1, "Novo nome"))
                .andExpect(status().isBadRequest())
                .andExpect(corpoContem("ativo"))
                .andExpect(corpoContem("habilitadoLicitar"));

        mvc.perform(get("/api/fornecedores/" + id))
                .andExpect(jsonPath("$.razaoSocial").value("Original"));
    }

    @Test
    void putSubstituiOsDadosELimpaOsOpcionaisOmitidos() throws Exception {
        cnaes.save(new Cnae(4711301, "Comercio varejista"));
        long id = criar(json(CNPJ_1, null, "Original", "true", "true", "\"cnaeCodigo\":4711301"));

        enviarPut(id, json(CNPJ_1, null, "Novo nome", "false", "false", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Novo nome"))
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.habilitadoLicitar").value(false))
                .andExpect(jsonPath("$.cnaeCodigo").isEmpty());
    }

    @Test
    void putInexistenteRetorna404EComCnpjDeOutroRetorna409() throws Exception {
        enviarPut(999, json(CNPJ_1, null, "X", "true", "false", null)).andExpect(status().isNotFound());

        criar(cnpjJson(CNPJ_1, "Um"));
        long segundo = criar(cnpjJson(CNPJ_2, "Dois"));

        enviarPut(segundo, json(CNPJ_1, null, "Dois", "true", "false", null)).andExpect(status().isConflict());
    }

    @Test
    void putMantendoOProprioDocumentoFunciona() throws Exception {
        long id = criar(cnpjJson(CNPJ_1, "Um"));

        enviarPut(id, json("04.252.011/0001-10", null, "Um atualizado", "true", "false", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Um atualizado"));
    }

    // -------------------------------------------------------------- exclusão

    @Test
    void excluiERetorna404NaSegundaVezEParaInexistente() throws Exception {
        long id = criar(cnpjJson(CNPJ_1, "Um"));

        mvc.perform(delete("/api/fornecedores/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/fornecedores/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/fornecedores/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/fornecedores/999")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------- filtros e paginação

    private void criarMassaDeFiltros() throws Exception {
        criar(json(CNPJ_1, null, "Alfa Industria", "true", "false", null));
        criar(json(CNPJ_2, null, "Alfa Comercio", "false", "false", null));
        criar(json(CNPJ_ALFA, null, "Beta Servicos", "true", "false", null));
    }

    @Test
    void filtrosBuscaEAtivoSaoAplicadosJuntos() throws Exception {
        criarMassaDeFiltros();

        mvc.perform(get("/api/fornecedores").param("busca", "alfa").param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Alfa Industria"));

        mvc.perform(get("/api/fornecedores").param("busca", "ALFA").param("ativo", "false"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Alfa Comercio"));

        mvc.perform(get("/api/fornecedores").param("busca", "beta").param("ativo", "false"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtrosIsoladosESemFiltro() throws Exception {
        criarMassaDeFiltros();

        mvc.perform(get("/api/fornecedores")).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/fornecedores").param("busca", "alfa")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/fornecedores").param("ativo", "false")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/fornecedores").param("ativo", "talvez")).andExpect(status().isBadRequest());
    }

    @Test
    void caracteresCuringaDaBuscaSaoLiterais() throws Exception {
        criarMassaDeFiltros();

        mvc.perform(get("/api/fornecedores").param("busca", "%")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/fornecedores").param("busca", "_")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginacaoInvalidaRetorna400() throws Exception {
        mvc.perform(get("/api/fornecedores").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fornecedores").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fornecedores").param("size", "-5")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fornecedores").param("size", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fornecedores").param("sort", "campoInexistente")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fornecedores").param("sort", "desc")).andExpect(status().isBadRequest());
    }

    @Test
    void tamanhoMaximoELimitadoA100() throws Exception {
        mvc.perform(get("/api/fornecedores").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void paginaEOrdenacaoSaoConsistentes() throws Exception {
        criarMassaDeFiltros();

        mvc.perform(get("/api/fornecedores").param("size", "2").param("sort", "razaoSocial,desc"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Beta Servicos"))
                .andExpect(jsonPath("$.content[1].razaoSocial").value("Alfa Industria"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/fornecedores").param("size", "2").param("page", "1").param("sort", "razaoSocial,desc"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Alfa Comercio"));

        mvc.perform(get("/api/fornecedores").param("page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mvc.perform(get("/api/fornecedores"))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Alfa Comercio"));
    }

    // ---------------------------------------------- endereços e municípios

    @Test
    void cadastraComEnderecoENormalizaMunicipio() throws Exception {
        long id = criar(json(CNPJ_1, null, "Com endereco", null, null, ENDERECO_JP));

        mvc.perform(get("/api/fornecedores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enderecos.length()").value(1))
                .andExpect(jsonPath("$.enderecos[0].logradouro").value("das Flores"))
                .andExpect(jsonPath("$.enderecos[0].cep").value("58000123"))
                .andExpect(jsonPath("$.enderecos[0].municipio").value("JOAO PESSOA"))
                .andExpect(jsonPath("$.enderecos[0].uf").value("PB"))
                .andExpect(jsonPath("$.enderecos[0].fonteDados").value("MANUAL"));
    }

    @Test
    void naoDuplicaMunicipioEntreFornecedores() throws Exception {
        criar(json(CNPJ_1, null, "Um", null, null, ENDERECO_JP));
        criar(json(CNPJ_2, null, "Dois", null, null,
                "\"enderecos\":[{\"logradouro\":\"Av. Central\",\"municipio\":\"  joao   pessoa \",\"uf\":\"PB\"}]"));

        assertEquals(1, municipios.count());
        assertEquals(2, enderecos.count());
    }

    @Test
    void listagemTambemRetornaEnderecos() throws Exception {
        criar(json(CNPJ_1, null, "Com endereco", null, null, ENDERECO_JP));

        mvc.perform(get("/api/fornecedores"))
                .andExpect(jsonPath("$.content[0].enderecos.length()").value(1))
                .andExpect(jsonPath("$.content[0].enderecos[0].municipio").value("JOAO PESSOA"));
    }

    @Test
    void putSubstituiEnderecosSemDuplicar() throws Exception {
        long id = criar(json(CNPJ_1, null, "Um", null, null, ENDERECO_JP));
        assertEquals(1, enderecos.count());

        enviarPut(id, json(CNPJ_1, null, "Um", "true", "false",
                "\"enderecos\":[{\"logradouro\":\"Rua Nova\",\"municipio\":\"Campina Grande\",\"uf\":\"PB\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enderecos.length()").value(1))
                .andExpect(jsonPath("$.enderecos[0].logradouro").value("Rua Nova"))
                .andExpect(jsonPath("$.enderecos[0].municipio").value("CAMPINA GRANDE"));

        assertEquals(1, enderecos.count());
        assertEquals(1, enderecos.findByFornecedorId(id).size());

        enviarPut(id, json(CNPJ_1, null, "Um", "true", "false", null))
                .andExpect(jsonPath("$.enderecos.length()").value(0));
        assertEquals(0, enderecos.count());
    }

    @Test
    void exclusaoRemoveEnderecosMasPreservaMunicipios() throws Exception {
        long id = criar(json(CNPJ_1, null, "Um", null, null, ENDERECO_JP));

        mvc.perform(delete("/api/fornecedores/" + id)).andExpect(status().isNoContent());

        assertEquals(0, enderecos.count());
        assertEquals(1, municipios.count());
    }

    @Test
    void rejeitaEnderecosInvalidos() throws Exception {
        enviarPost(json(CNPJ_1, null, "X", null, null, "\"enderecos\":[{\"logradouro\":\"Rua\",\"cep\":\"123\"}]"))
                .andExpect(status().isBadRequest()).andExpect(corpoContem("cep"));
        enviarPost(json(CNPJ_1, null, "X", null, null, "\"enderecos\":[{\"logradouro\":\"Rua\",\"municipio\":\"Joao\",\"uf\":\"PBX\"}]"))
                .andExpect(status().isBadRequest()).andExpect(corpoContem("uf"));
        enviarPost(json(CNPJ_1, null, "X", null, null, "\"enderecos\":[{\"logradouro\":\"Rua\",\"municipio\":\"Joao\"}]"))
                .andExpect(status().isBadRequest()).andExpect(corpoContem("Informe município e UF juntos"));
        enviarPost(json(CNPJ_1, null, "X", null, null, "\"enderecos\":[{\"bairro\":\"Centro\"}]"))
                .andExpect(status().isBadRequest());

        assertEquals(0, fornecedores.count());
        assertEquals(0, municipios.count());
    }
}
