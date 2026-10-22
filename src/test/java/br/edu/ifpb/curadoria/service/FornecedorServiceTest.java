package br.edu.ifpb.curadoria.service;

import br.edu.ifpb.curadoria.dto.EnderecoRequest;
import br.edu.ifpb.curadoria.dto.FornecedorRequest;
import br.edu.ifpb.curadoria.dto.FornecedorResponse;
import br.edu.ifpb.curadoria.entity.Endereco;
import br.edu.ifpb.curadoria.entity.Fornecedor;
import br.edu.ifpb.curadoria.entity.Municipio;
import br.edu.ifpb.curadoria.repository.CnaeRepository;
import br.edu.ifpb.curadoria.repository.FornecedorRepository;
import br.edu.ifpb.curadoria.repository.MunicipioRepository;
import br.edu.ifpb.curadoria.repository.NaturezaJuridicaRepository;
import br.edu.ifpb.curadoria.repository.PorteEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock
    private FornecedorRepository fornecedores;

    @Mock
    private CnaeRepository cnaes;

    @Mock
    private NaturezaJuridicaRepository naturezas;

    @Mock
    private PorteEmpresaRepository portes;

    @Mock
    private MunicipioRepository municipios;

    private FornecedorService service;

    @BeforeEach
    void configurar() {
        service = new FornecedorService(
                fornecedores,
                cnaes,
                naturezas,
                portes,
                municipios
        );
    }

    @Test
    void cadastraFornecedorComCnpj() {
        FornecedorRequest request = request(
                "04.252.011/0001-10",
                null
        );

        when(fornecedores.existsByCnpj("04252011000110"))
                .thenReturn(false);

        when(fornecedores.save(any(Fornecedor.class)))
                .thenAnswer(invocacao -> {
                    Fornecedor fornecedor = invocacao.getArgument(0);
                    return fornecedor;
                });

        FornecedorResponse response = service.criar(request);

        assertEquals("04252011000110", response.cnpj());
        assertNull(response.cpf());
        verify(fornecedores).save(any(Fornecedor.class));
    }

    @Test
    void cadastraFornecedorComCpf() {
        FornecedorRequest request = request(
                null,
                "529.982.247-25"
        );

        when(fornecedores.existsByCpf("52998224725"))
                .thenReturn(false);

        when(fornecedores.save(any(Fornecedor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        FornecedorResponse response = service.criar(request);

        assertNull(response.cnpj());
        assertEquals("52998224725", response.cpf());
        verify(fornecedores).save(any(Fornecedor.class));
    }

    @Test
    void rejeitaCnpjDuplicado() {
        FornecedorRequest request = request(
                "04.252.011/0001-10",
                null
        );

        when(fornecedores.existsByCnpj("04252011000110"))
                .thenReturn(true);

        assertThrows(
                ConflitoException.class,
                () -> service.criar(request)
        );

        verify(fornecedores, never()).save(any(Fornecedor.class));
    }

    @Test
    void rejeitaCpfDuplicado() {
        FornecedorRequest request = request(
                null,
                "529.982.247-25"
        );

        when(fornecedores.existsByCpf("52998224725"))
                .thenReturn(true);

        assertThrows(
                ConflitoException.class,
                () -> service.criar(request)
        );

        verify(fornecedores, never()).save(any(Fornecedor.class));
    }

    @Test
    void atualizaFornecedorMantendoSeuCnpj() {
        Fornecedor fornecedor = new Fornecedor(
                "04252011000110",
                null,
                "Fornecedor Antigo",
                true,
                false
        );

        setId(fornecedor, 1L);

        when(fornecedores.findById(1L))
                .thenReturn(Optional.of(fornecedor));

        when(fornecedores.findByCnpj("04252011000110"))
                .thenReturn(Optional.of(fornecedor));

        FornecedorResponse response = service.atualizar(
                1L,
                request("04.252.011/0001-10", null)
        );

        assertEquals("04252011000110", response.cnpj());
        assertEquals("Fornecedor Teste", response.razaoSocial());
        assertNull(response.cpf());
    }

    @Test
    void rejeitaAtualizacaoComCnpjDeOutroFornecedor() {
        Fornecedor atual = new Fornecedor(
                "11222333000181",
                null,
                "Fornecedor Atual",
                true,
                false
        );

        Fornecedor outro = new Fornecedor(
                "04252011000110",
                null,
                "Outro Fornecedor",
                true,
                false
        );

        setId(atual, 1L);
        setId(outro, 2L);

        when(fornecedores.findById(1L))
                .thenReturn(Optional.of(atual));

        when(fornecedores.findByCnpj("04252011000110"))
                .thenReturn(Optional.of(outro));

        assertThrows(
                ConflitoException.class,
                () -> service.atualizar(
                        1L,
                        request("04.252.011/0001-10", null)
                )
        );
    }

    @Test
    void rejeitaAtualizacaoComCpfDeOutroFornecedor() {
        Fornecedor atual = new Fornecedor(
                null,
                "11144477735",
                "Fornecedor Atual",
                true,
                false
        );

        Fornecedor outro = new Fornecedor(
                null,
                "52998224725",
                "Outro Fornecedor",
                true,
                false
        );

        setId(atual, 1L);
        setId(outro, 2L);

        when(fornecedores.findById(1L))
                .thenReturn(Optional.of(atual));

        when(fornecedores.findByCpf("52998224725"))
                .thenReturn(Optional.of(outro));

        assertThrows(
                ConflitoException.class,
                () -> service.atualizar(
                        1L,
                        request(null, "529.982.247-25")
                )
        );
    }

    @Test
    void rejeitaFornecedorSemDocumentoNoServico() {
        assertThrows(
                ParametroInvalidoException.class,
                () -> service.criar(request(null, null))
        );

        verify(fornecedores, never()).save(any(Fornecedor.class));
    }

    @Test
    void rejeitaCnaeInexistente() {
        FornecedorRequest request = new FornecedorRequest(
                "04.252.011/0001-10", null, "Fornecedor Teste",
                true, false, 9999, null, null, null);

        when(cnaes.findById(9999)).thenReturn(Optional.empty());

        assertThrows(
                ReferenciaInvalidaException.class,
                () -> service.criar(request)
        );

        verify(fornecedores, never()).save(any(Fornecedor.class));
    }

    @Test
    void atualizacaoDeFornecedorInexistenteLancaNaoEncontrado() {
        when(fornecedores.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.atualizar(99L, request("04.252.011/0001-10", null))
        );
    }

    @Test
    void exclusaoDeFornecedorInexistenteLancaNaoEncontrado() {
        when(fornecedores.existsById(99L)).thenReturn(false);

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluir(99L)
        );

        verify(fornecedores, never()).deleteById(any());
    }

    @Test
    void reutilizaMunicipioExistenteSemCriarOutro() {
        Municipio existente = new Municipio("JOAO PESSOA", "PB");

        when(municipios.findByNomeAndUf("JOAO PESSOA", "PB"))
                .thenReturn(Optional.of(existente));
        when(fornecedores.save(any(Fornecedor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        FornecedorResponse response = service.criar(
                requestComEnderecos(List.of(endereco(" João  Pessoa ", "pb"))));

        assertEquals(1, response.enderecos().size());
        assertEquals("JOAO PESSOA", response.enderecos().get(0).municipio());
        assertEquals("PB", response.enderecos().get(0).uf());
        assertEquals("58000123", response.enderecos().get(0).cep());
        assertEquals("MANUAL", response.enderecos().get(0).fonteDados());
        verify(municipios, never()).save(any(Municipio.class));
    }

    @Test
    void criaMunicipioQuandoAindaNaoExiste() {
        when(municipios.findByNomeAndUf("CAMPINA GRANDE", "PB"))
                .thenReturn(Optional.empty());
        when(municipios.save(any(Municipio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(fornecedores.save(any(Fornecedor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        FornecedorResponse response = service.criar(
                requestComEnderecos(List.of(endereco("Campina Grande", "PB"))));

        assertEquals("CAMPINA GRANDE", response.enderecos().get(0).municipio());
        verify(municipios).save(any(Municipio.class));
    }

    @Test
    void atualizacaoSubstituiEnderecosSemDuplicar() {
        Fornecedor existente = new Fornecedor(
                "04252011000110", null, "Fornecedor Antigo", true, false);
        setId(existente, 1L);

        Endereco antigo = new Endereco(existente);
        antigo.setLogradouro("Rua Antiga");
        existente.adicionarEndereco(antigo);

        when(fornecedores.findById(1L)).thenReturn(Optional.of(existente));
        when(fornecedores.findByCnpj("04252011000110"))
                .thenReturn(Optional.of(existente));
        when(municipios.findByNomeAndUf("JOAO PESSOA", "PB"))
                .thenReturn(Optional.of(new Municipio("JOAO PESSOA", "PB")));

        FornecedorResponse response = service.atualizar(
                1L,
                requestComEnderecos(List.of(
                        endereco("João Pessoa", "PB"),
                        endereco("João Pessoa", "PB"))));

        assertEquals(2, response.enderecos().size());
        assertTrue(response.enderecos().stream()
                .noneMatch(e -> "Rua Antiga".equals(e.logradouro())));
        assertEquals(2, existente.getEnderecos().size());
    }

    @Test
    void atualizacaoSemEnderecosLimpaALista() {
        Fornecedor existente = new Fornecedor(
                "04252011000110", null, "Fornecedor Antigo", true, false);
        setId(existente, 1L);
        existente.adicionarEndereco(new Endereco(existente));

        when(fornecedores.findById(1L)).thenReturn(Optional.of(existente));
        when(fornecedores.findByCnpj("04252011000110"))
                .thenReturn(Optional.of(existente));

        FornecedorResponse response = service.atualizar(
                1L, request("04.252.011/0001-10", null));

        assertTrue(response.enderecos().isEmpty());
    }

    @Test
    void rejeitaMunicipioSemUfNoServico() {
        assertThrows(
                ParametroInvalidoException.class,
                () -> service.criar(requestComEnderecos(
                        List.of(endereco("João Pessoa", null))))
        );

        verify(fornecedores, never()).save(any(Fornecedor.class));
    }

    @Test
    void escapaCaracteresCuringaDoLike() {
        assertEquals("100\\%\\_a\\\\b", FornecedorService.escaparLike("100%_a\\b"));
    }

    private FornecedorRequest requestComEnderecos(List<EnderecoRequest> enderecos) {
        return new FornecedorRequest(
                "04.252.011/0001-10", null, "Fornecedor Teste",
                true, false, null, null, null, enderecos);
    }

    private EnderecoRequest endereco(String municipio, String uf) {
        return new EnderecoRequest(
                "Rua", "das Flores", "10", null, "Centro",
                "58000-123", municipio, uf);
    }

    private FornecedorRequest request(String cnpj, String cpf) {
        return new FornecedorRequest(
                cnpj,
                cpf,
                "Fornecedor Teste",
                true,
                false,
                null,
                null,
                null,
                null
        );
    }

    private void setId(Fornecedor fornecedor, Long id) {
        try {
            var campo = Fornecedor.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(fornecedor, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}