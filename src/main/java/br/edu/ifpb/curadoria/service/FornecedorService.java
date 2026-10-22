package br.edu.ifpb.curadoria.service;

import br.edu.ifpb.curadoria.dto.*;
import br.edu.ifpb.curadoria.entity.*;
import br.edu.ifpb.curadoria.repository.*;
import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class FornecedorService {

    static final String FONTE_MANUAL = "MANUAL";

    private final FornecedorRepository fornecedores;
    private final CnaeRepository cnaes;
    private final NaturezaJuridicaRepository naturezas;
    private final PorteEmpresaRepository portes;
    private final MunicipioRepository municipios;

    public FornecedorService(
            FornecedorRepository fornecedores,
            CnaeRepository cnaes,
            NaturezaJuridicaRepository naturezas,
            PorteEmpresaRepository portes,
            MunicipioRepository municipios) {
        this.fornecedores = fornecedores;
        this.cnaes = cnaes;
        this.naturezas = naturezas;
        this.portes = portes;
        this.municipios = municipios;
    }


    public Page<FornecedorResponse> listar(
            String busca,
            Boolean ativo,
            Pageable pageable) {
        return fornecedores.findAll(filtro(busca, ativo), pageable).map(this::response);
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
        String cnpj = documentoOuNulo(request.cnpj());
        String cpf = documentoOuNulo(request.cpf());

        validarIdentificacao(cnpj, cpf);
        String razaoSocial = razaoSocialObrigatoria(request.razaoSocial());

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
                razaoSocial,
                request.ativo() == null || request.ativo(),
                request.habilitadoLicitar() != null
                        && request.habilitadoLicitar());

        aplicarReferencias(fornecedor, request);
        substituirEnderecos(fornecedor, request.enderecos());

        return response(fornecedores.save(fornecedor));
    }


    public FornecedorResponse atualizar(
            Long id,
            FornecedorRequest request) {
        Fornecedor fornecedor = fornecedores.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado: " + id));

        String cnpj = documentoOuNulo(request.cnpj());
        String cpf = documentoOuNulo(request.cpf());

        validarIdentificacao(cnpj, cpf);
        String razaoSocial = razaoSocialObrigatoria(request.razaoSocial());

        if (cnpj != null) {
            fornecedores.findByCnpj(cnpj)
                    .filter(outro -> !Objects.equals(outro.getId(), id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Já existe fornecedor com este CNPJ");
                    });
        }

        if (cpf != null) {
            fornecedores.findByCpf(cpf)
                    .filter(outro -> !Objects.equals(outro.getId(), id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Já existe fornecedor com este CPF");
                    });
        }

        fornecedor.setCnpj(cnpj);
        fornecedor.setCpf(cpf);
        fornecedor.setRazaoSocial(razaoSocial);
        fornecedor.setAtivo(
                request.ativo() == null || request.ativo());
        fornecedor.setHabilitadoLicitar(
                request.habilitadoLicitar() != null
                        && request.habilitadoLicitar());

        aplicarReferencias(fornecedor, request);
        substituirEnderecos(fornecedor, request.enderecos());


        fornecedores.flush();

        return response(fornecedor);
    }

    public void excluir(Long id) {
        if (!fornecedores.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Fornecedor não encontrado: " + id);
        }

        fornecedores.deleteById(id);
    }



    private static Specification<Fornecedor> filtro(String busca, Boolean ativo) {
        String termo = DadosNormalizer.texto(busca);

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (termo != null) {
                String padrao = "%" + escaparLike(termo.toLowerCase(Locale.ROOT)) + "%";
                predicados.add(cb.like(cb.lower(root.<String>get("razaoSocial")), padrao, '\\'));
            }

            if (ativo != null) {
                predicados.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }


    static String escaparLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }



    private static String documentoOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String documento = DadosNormalizer.documento(valor);
        return documento.isEmpty() ? null : documento;
    }


    private static void validarIdentificacao(String cnpj, String cpf) {
        if ((cnpj == null) == (cpf == null)) {
            throw new ParametroInvalidoException(
                    "Informe apenas um documento: CPF ou CNPJ");
        }
    }

    private static String razaoSocialObrigatoria(String razaoSocial) {
        String normalizada = DadosNormalizer.texto(razaoSocial);
        if (normalizada == null) {
            throw new ParametroInvalidoException("Razão social é obrigatória");
        }
        return normalizada;
    }



    private void aplicarReferencias(
            Fornecedor fornecedor,
            FornecedorRequest request) {
        fornecedor.setCnae(
                request.cnaeCodigo() == null
                        ? null
                        : cnaes.findById(request.cnaeCodigo())
                        .orElseThrow(() ->
                                new ReferenciaInvalidaException(
                                        "CNAE não encontrado: "
                                                + request.cnaeCodigo())));

        fornecedor.setNaturezaJuridica(
                request.naturezaJuridicaCodigo() == null
                        ? null
                        : naturezas.findById(
                                request.naturezaJuridicaCodigo())
                        .orElseThrow(() ->
                                new ReferenciaInvalidaException(
                                        "Natureza jurídica não encontrada: "
                                                + request.naturezaJuridicaCodigo())));

        fornecedor.setPorteEmpresa(
                request.porteEmpresaCodigo() == null
                        ? null
                        : portes.findById(request.porteEmpresaCodigo())
                        .orElseThrow(() ->
                                new ReferenciaInvalidaException(
                                        "Porte de empresa não encontrado: "
                                                + request.porteEmpresaCodigo())));
    }


    private void substituirEnderecos(
            Fornecedor fornecedor,
            List<EnderecoRequest> requests) {
        fornecedor.getEnderecos().clear();

        if (requests == null) {
            return;
        }

        for (EnderecoRequest request : requests) {
            Endereco endereco = new Endereco(fornecedor);
            endereco.setTipoLogradouro(DadosNormalizer.texto(request.tipoLogradouro()));
            endereco.setLogradouro(DadosNormalizer.texto(request.logradouro()));
            endereco.setNumero(DadosNormalizer.texto(request.numero()));
            endereco.setComplemento(DadosNormalizer.texto(request.complemento()));
            endereco.setBairro(DadosNormalizer.texto(request.bairro()));
            endereco.setCep(DadosNormalizer.cep(request.cep()));
            endereco.setFonteDados(FONTE_MANUAL);
            endereco.setMunicipio(resolverMunicipio(request.municipio(), request.uf()));

            fornecedor.adicionarEndereco(endereco);
        }
    }


    private Municipio resolverMunicipio(String nome, String uf) {
        String nomeNormalizado = DadosNormalizer.municipio(nome);
        String ufNormalizada = DadosNormalizer.uf(uf);

        if (nomeNormalizado == null && ufNormalizada == null) {
            return null;
        }

        if (nomeNormalizado == null || ufNormalizada == null) {
            throw new ParametroInvalidoException("Informe município e UF juntos");
        }

        return municipios.findByNomeAndUf(nomeNormalizado, ufNormalizada)
                .orElseGet(() -> municipios.save(
                        new Municipio(nomeNormalizado, ufNormalizada)));
    }


    private EnderecoResponse enderecoResponse(Endereco endereco) {
        Municipio municipio = endereco.getMunicipio();

        return new EnderecoResponse(
                endereco.getId(),
                endereco.getTipoLogradouro(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCep(),
                municipio == null ? null : municipio.getNome(),
                municipio == null ? null : municipio.getUf(),
                endereco.getFonteDados());
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
                        ? null : fornecedor.getPorteEmpresa().getDescricao(),
                fornecedor.getEnderecos().stream()
                        .map(this::enderecoResponse)
                        .toList());
    }
}
