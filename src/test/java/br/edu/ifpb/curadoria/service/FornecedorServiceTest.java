package br.edu.ifpb.curadoria.service;

import br.edu.ifpb.curadoria.dto.FornecedorRequest;
import br.edu.ifpb.curadoria.dto.FornecedorResponse;
import br.edu.ifpb.curadoria.entity.Fornecedor;
import br.edu.ifpb.curadoria.repository.CnaeRepository;
import br.edu.ifpb.curadoria.repository.FornecedorRepository;
import br.edu.ifpb.curadoria.repository.NaturezaJuridicaRepository;
import br.edu.ifpb.curadoria.repository.PorteEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private FornecedorService service;

    @BeforeEach
    void configurar() {
        service = new FornecedorService(
                fornecedores,
                cnaes,
                naturezas,
                portes
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

    private FornecedorRequest request(String cnpj, String cpf) {
        return new FornecedorRequest(
                cnpj,
                cpf,
                "Fornecedor Teste",
                true,
                false,
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