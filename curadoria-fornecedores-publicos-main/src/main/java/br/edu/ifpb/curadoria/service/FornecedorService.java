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

    public FornecedorService(FornecedorRepository fornecedores, CnaeRepository cnaes,
            NaturezaJuridicaRepository naturezas, PorteEmpresaRepository portes) {
        this.fornecedores = fornecedores; this.cnaes = cnaes;
        this.naturezas = naturezas; this.portes = portes;
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(String busca, Boolean ativo, Pageable pageable) {
        Page<Fornecedor> page = busca != null && !busca.isBlank()
                ? fornecedores.findByRazaoSocialContainingIgnoreCase(DadosNormalizer.texto(busca), pageable)
                : ativo == null ? fornecedores.findAll(pageable) : fornecedores.findByAtivo(ativo, pageable);
        return page.map(this::response);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscar(Long id) { return response(fornecedores.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id))); }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorCnpj(String cnpj) {
        String normalizado = DadosNormalizer.documento(cnpj);
        return response(fornecedores.findByCnpj(normalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado para o CNPJ informado")));
    }

    public FornecedorResponse criar(FornecedorRequest request) {
        String cnpj = DadosNormalizer.documento(request.cnpj());
        if (fornecedores.existsByCnpj(cnpj)) throw new ConflitoException("Já existe fornecedor com este CNPJ");
        Fornecedor fornecedor = new Fornecedor(cnpj, DadosNormalizer.documento(request.cpf()),
                DadosNormalizer.texto(request.razaoSocial()), request.ativo() == null || request.ativo(),
                request.habilitadoLicitar() != null && request.habilitadoLicitar());
        aplicarReferencias(fornecedor, request);
        return response(fornecedores.save(fornecedor));
    }

    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = fornecedores.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
        String cnpj = DadosNormalizer.documento(request.cnpj());
        fornecedores.findByCnpj(cnpj).filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new ConflitoException("Já existe fornecedor com este CNPJ"); });
        fornecedor.setCnpj(cnpj); fornecedor.setCpf(DadosNormalizer.documento(request.cpf()));
        fornecedor.setRazaoSocial(DadosNormalizer.texto(request.razaoSocial()));
        fornecedor.setAtivo(request.ativo() == null || request.ativo());
        fornecedor.setHabilitadoLicitar(request.habilitadoLicitar() != null && request.habilitadoLicitar());
        aplicarReferencias(fornecedor, request);
        return response(fornecedor);
    }

    public void excluir(Long id) {
        if (!fornecedores.existsById(id)) throw new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id);
        fornecedores.deleteById(id);
    }

    private void aplicarReferencias(Fornecedor f, FornecedorRequest r) {
        f.setCnae(r.cnaeCodigo() == null ? null : cnaes.findById(r.cnaeCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CNAE não encontrado: " + r.cnaeCodigo())));
        f.setNaturezaJuridica(r.naturezaJuridicaCodigo() == null ? null : naturezas.findById(r.naturezaJuridicaCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Natureza jurídica não encontrada: " + r.naturezaJuridicaCodigo())));
        f.setPorteEmpresa(r.porteEmpresaCodigo() == null ? null : portes.findById(r.porteEmpresaCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Porte de empresa não encontrado: " + r.porteEmpresaCodigo())));
    }

    private FornecedorResponse response(Fornecedor f) {
        return new FornecedorResponse(f.getId(), f.getCnpj(), f.getCpf(), f.getRazaoSocial(), f.getAtivo(),
                f.getHabilitadoLicitar(), f.getCnae() == null ? null : f.getCnae().getCodigo(),
                f.getCnae() == null ? null : f.getCnae().getDescricao(),
                f.getNaturezaJuridica() == null ? null : f.getNaturezaJuridica().getCodigo(),
                f.getNaturezaJuridica() == null ? null : f.getNaturezaJuridica().getDescricao(),
                f.getPorteEmpresa() == null ? null : f.getPorteEmpresa().getCodigo(),
                f.getPorteEmpresa() == null ? null : f.getPorteEmpresa().getDescricao());
    }
}
