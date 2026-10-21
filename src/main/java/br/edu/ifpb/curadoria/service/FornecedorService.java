package br.edu.ifpb.curadoria.service;

import br.edu.ifpb.curadoria.dto.*;
import br.edu.ifpb.curadoria.entity.*;
import br.edu.ifpb.curadoria.repository.*;
import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FornecedorService {

    private final FornecedorRepository fornecedores;
    private final CnaeRepository cnaes;
    private final NaturezaJuridicaRepository naturezas;
    private final PorteEmpresaRepository portes;

    public FornecedorService(
            FornecedorRepository fornecedores,
            CnaeRepository cnaes,
            NaturezaJuridicaRepository naturezas,
            PorteEmpresaRepository portes) {
        this.fornecedores = fornecedores;
        this.cnaes = cnaes;
        this.naturezas = naturezas;
        this.portes = portes;
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(
            String busca,
            Boolean ativo,
            Pageable pageable) {
        Page<Fornecedor> page = busca != null && !busca.isBlank()
                ? fornecedores.findByRazaoSocialContainingIgnoreCase(
                DadosNormalizer.texto(busca), pageable)
                : ativo == null
                ? fornecedores.findAll(pageable)
                : fornecedores.findByAtivo(ativo, pageable);

        return page.map(this::response);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscar(Long id) {
        return response(fornecedores.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorCnpj(String cnpj) {
        String normalizado = DadosNormalizer.documento(cnpj);

        return response(fornecedores.findByCnpj(normalizado)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado para o CNPJ informado")));
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorCpf(String cpf) {
        String normalizado = DadosNormalizer.documento(cpf);

        return response(fornecedores.findByCpf(normalizado)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado para o CPF informado")));
    }

    public FornecedorResponse criar(FornecedorRequest request) {
        String cnpj = request.cnpj() == null
                || request.cnpj().isBlank()
                ? null
                : DadosNormalizer.documento(request.cnpj());

        String cpf = request.cpf() == null
                || request.cpf().isBlank()
                ? null
                : DadosNormalizer.documento(request.cpf());

        if (cnpj != null && fornecedores.existsByCnpj(cnpj)) {
            throw new ConflitoException(
                    "Já existe fornecedor com este CNPJ");
        }

        if (cpf != null && fornecedores.existsByCpf(cpf)) {
            throw new ConflitoException(
                    "Já existe fornecedor com este CPF");
        }

        Fornecedor fornecedor = new Fornecedor(
                cnpj,
                cpf,
                DadosNormalizer.texto(request.razaoSocial()),
                request.ativo() == null || request.ativo(),
                request.habilitadoLicitar() != null
                        && request.habilitadoLicitar());

        aplicarReferencias(fornecedor, request);

        return response(fornecedores.save(fornecedor));
    }

    public FornecedorResponse atualizar(
            Long id,
            FornecedorRequest request) {
        Fornecedor fornecedor = fornecedores.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado: " + id));

        String cnpj = request.cnpj() == null
                || request.cnpj().isBlank()
                ? null
                : DadosNormalizer.documento(request.cnpj());

        String cpf = request.cpf() == null
                || request.cpf().isBlank()
                ? null
                : DadosNormalizer.documento(request.cpf());

        if (cnpj != null) {
            fornecedores.findByCnpj(cnpj)
                    .filter(outro -> !java.util.Objects.equals(outro.getId(), id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Já existe fornecedor com este CNPJ");
                    });
        }

        if (cpf != null) {
            fornecedores.findByCpf(cpf)
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Já existe fornecedor com este CPF");
                    });
        }

        fornecedor.setCnpj(cnpj);
        fornecedor.setCpf(cpf);
        fornecedor.setRazaoSocial(
                DadosNormalizer.texto(request.razaoSocial()));
        fornecedor.setAtivo(
                request.ativo() == null || request.ativo());
        fornecedor.setHabilitadoLicitar(
                request.habilitadoLicitar() != null
                        && request.habilitadoLicitar());

        aplicarReferencias(fornecedor, request);

        return response(fornecedor);
    }

    public void excluir(Long id) {
        if (!fornecedores.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Fornecedor não encontrado: " + id);
        }

        fornecedores.deleteById(id);
    }

    private void aplicarReferencias(
            Fornecedor fornecedor,
            FornecedorRequest request) {
        fornecedor.setCnae(
                request.cnaeCodigo() == null
                        ? null
                        : cnaes.findById(request.cnaeCodigo())
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "CNAE não encontrado: "
                                                + request.cnaeCodigo())));

        fornecedor.setNaturezaJuridica(
                request.naturezaJuridicaCodigo() == null
                        ? null
                        : naturezas.findById(
                                request.naturezaJuridicaCodigo())
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Natureza jurídica não encontrada: "
                                                + request.naturezaJuridicaCodigo())));

        fornecedor.setPorteEmpresa(
                request.porteEmpresaCodigo() == null
                        ? null
                        : portes.findById(request.porteEmpresaCodigo())
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Porte de empresa não encontrado: "
                                                + request.porteEmpresaCodigo())));
    }

    private FornecedorResponse response(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getCnpj(),
                fornecedor.getCpf(),
                fornecedor.getRazaoSocial(),
                fornecedor.getAtivo(),
                fornecedor.getHabilitadoLicitar(),
                fornecedor.getCnae() == null
                        ? null : fornecedor.getCnae().getCodigo(),
                fornecedor.getCnae() == null
                        ? null : fornecedor.getCnae().getDescricao(),
                fornecedor.getNaturezaJuridica() == null
                        ? null : fornecedor.getNaturezaJuridica().getCodigo(),
                fornecedor.getNaturezaJuridica() == null
                        ? null : fornecedor.getNaturezaJuridica().getDescricao(),
                fornecedor.getPorteEmpresa() == null
                        ? null : fornecedor.getPorteEmpresa().getCodigo(),
                fornecedor.getPorteEmpresa() == null
                        ? null : fornecedor.getPorteEmpresa().getDescricao());
    }
}