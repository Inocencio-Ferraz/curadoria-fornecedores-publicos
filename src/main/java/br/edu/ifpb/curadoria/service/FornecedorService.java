package br.edu.ifpb.curadoria.service;

import br.edu.ifpb.curadoria.dto.*;
import br.edu.ifpb.curadoria.entity.*;
import br.edu.ifpb.curadoria.repository.*;
import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class FornecedorService {

    private static final Logger log = LoggerFactory.getLogger(FornecedorService.class);

    static final String FONTE_MANUAL = "MANUAL";
    static final String FONTE_COMPRAS_GOV = "COMPRAS_GOV";
    static final String FONTE_BRASIL_API = "BRASIL_API";
    private final ComprasGovService comprasGovService;
    private final FornecedorRepository fornecedores;
    private final CnaeRepository cnaes;
    private final NaturezaJuridicaRepository naturezas;
    private final PorteEmpresaRepository portes;
    private final MunicipioRepository municipios;
    private final BrasilApiService brasilApiService;

    public FornecedorService(
            FornecedorRepository fornecedores,
            CnaeRepository cnaes,
            NaturezaJuridicaRepository naturezas,
            PorteEmpresaRepository portes,
            MunicipioRepository municipios, ComprasGovService comprasGovService, BrasilApiService brasilApiService) {
        this.fornecedores = fornecedores;
        this.cnaes = cnaes;
        this.naturezas = naturezas;
        this.portes = portes;
        this.municipios = municipios;
        this.comprasGovService = comprasGovService;
        this.brasilApiService = brasilApiService;
    }

    /**
     * Importa um fornecedor pelo CNPJ.
     * ComprasGov é a fonte principal; a BrasilAPI é auxiliar e só completa o que o ComprasGov não trouxe
     * (razão social, CNAE, natureza jurídica, porte e endereço).
     */
    @Transactional
    public FornecedorResponse importarDoComprasGov(String cnpj) {
        String cnpjLimpo = cnpj != null ? cnpj.replaceAll("\\D", "") : "";

        Map<String, Object> compras = Map.of();
        Map<String, Object> brasil = Map.of();

        try {
            Map<String, Object> resposta = comprasGovService.consultarFornecedorPorCnpj(cnpjLimpo);
            if (resposta != null) compras = resposta;
        } catch (Exception e) {
            log.warn("ComprasGov indisponível para o CNPJ {}: {}", cnpjLimpo, e.getMessage());
        }

        try {
            Map<String, Object> resposta = brasilApiService.consultarCnpj(cnpjLimpo);
            if (resposta != null) brasil = resposta;
        } catch (Exception e) {
            log.warn("BrasilAPI indisponível para o CNPJ {}: {}", cnpjLimpo, e.getMessage());
        }

        if (compras.isEmpty() && brasil.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum dado encontrado nas APIs externas para o CNPJ: " + cnpj);
        }

        boolean noComprasGov = !compras.isEmpty();

        String razaoSocial = texto(compras, "nomeRazaoSocialFornecedor", "nomeRazaoSocial", "razaoSocial", "nome");
        if (razaoSocial == null) razaoSocial = texto(brasil, "razao_social");
        if (razaoSocial == null) razaoSocial = "FORNECEDOR SEM NOME";

        // Situação no Compras vem do próprio ComprasGov; sem registro lá, não está habilitado a licitar
        boolean ativo = noComprasGov
                ? booleano(compras, "ativo", true)
                : !"BAIXADA".equalsIgnoreCase(texto(brasil, "descricao_situacao_cadastral"));
        boolean habilitadoLicitar = noComprasGov && booleano(compras, "habilitadoLicitar", true);

        // CNAE, natureza jurídica e porte: ComprasGov primeiro, BrasilAPI como complemento
        Integer cnae = inteiro(compras, "codigoCnae", "cnae");
        String cnaeDescricao = texto(compras, "nomeCnae", "descricaoCnae");
        if (cnae == null) {
            cnae = inteiro(brasil, "cnae_fiscal");
            cnaeDescricao = texto(brasil, "cnae_fiscal_descricao");
        }

        Integer natureza = inteiro(compras, "codigoNaturezaJuridica", "naturezaJuridica");
        String naturezaDescricao = texto(compras, "nomeNaturezaJuridica", "descricaoNaturezaJuridica");
        if (natureza == null) {
            natureza = inteiro(brasil, "codigo_natureza_juridica");
            naturezaDescricao = texto(brasil, "natureza_juridica");
        }

        Integer porte = inteiro(compras, "codigoPorteEmpresa", "porteEmpresa");
        String porteDescricao = texto(compras, "nomePorteEmpresa", "descricaoPorteEmpresa");
        if (porte == null) {
            porte = inteiro(brasil, "codigo_porte");
            porteDescricao = texto(brasil, "porte");
        }

        // As tabelas de referência começam vazias: cadastra o código se ainda não existir
        if (cnae != null && !cnaes.existsById(cnae)) {
            cnaes.save(new Cnae(cnae, descricaoOuPadrao(cnaeDescricao, 300)));
        }
        if (natureza != null && !naturezas.existsById(natureza)) {
            naturezas.save(new NaturezaJuridica(natureza, descricaoOuPadrao(naturezaDescricao, 200)));
        }
        if (porte != null && !portes.existsById(porte)) {
            portes.save(new PorteEmpresa(porte, descricaoOuPadrao(porteDescricao, 100)));
        }

        // Endereço: a BrasilAPI traz o endereço completo; o ComprasGov entra se ela não tiver
        List<EnderecoRequest> enderecos = new ArrayList<>();
        String fonteEndereco = FONTE_BRASIL_API;
        EnderecoRequest endereco = enderecoBrasilApi(brasil);
        if (endereco == null) {
            endereco = enderecoComprasGov(compras);
            fonteEndereco = FONTE_COMPRAS_GOV;
        }
        if (endereco != null) enderecos.add(endereco);

        FornecedorRequest requestDto = new FornecedorRequest(
                cnpjLimpo, null, DadosNormalizer.texto(razaoSocial), ativo, habilitadoLicitar,
                cnae, natureza, porte, enderecos
        );

        return criar(requestDto, fonteEndereco);
    }


    private static EnderecoRequest enderecoBrasilApi(Map<String, Object> d) {
        return montarEndereco(
                texto(d, "descricao_tipo_de_logradouro"), texto(d, "logradouro"), texto(d, "numero"),
                texto(d, "complemento"), texto(d, "bairro"), texto(d, "cep"),
                texto(d, "municipio"), texto(d, "uf"));
    }

    private static EnderecoRequest enderecoComprasGov(Map<String, Object> d) {
        return montarEndereco(
                null, null, null, null, null, texto(d, "cep"),
                texto(d, "nomeMunicipio", "municipio"), texto(d, "ufSigla", "uf"));
    }

    private static EnderecoRequest montarEndereco(
            String tipo, String logradouro, String numero, String complemento,
            String bairro, String cep, String municipio, String uf) {
        // município e UF só valem juntos
        if (municipio == null || uf == null || uf.length() != 2) {
            municipio = null;
            uf = null;
        }
        if (cep != null && !DadosNormalizer.cep(cep).matches("\\d{8}")) {
            cep = null;
        }
        if (logradouro == null && cep == null && municipio == null) {
            return null;
        }
        return new EnderecoRequest(tipo, logradouro, numero, complemento, bairro, cep, municipio, uf);
    }


    private static String texto(Map<String, Object> dados, String... chaves) {
        for (String chave : chaves) {
            Object valor = dados.get(chave);
            if (valor != null) {
                String texto = DadosNormalizer.texto(String.valueOf(valor));
                if (texto != null) return texto;
            }
        }
        return null;
    }

    private static Integer inteiro(Map<String, Object> dados, String... chaves) {
        String valor = texto(dados, chaves);
        if (valor == null) return null;
        try {
            return Integer.valueOf(valor.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean booleano(Map<String, Object> dados, String chave, boolean padrao) {
        String valor = texto(dados, chave);
        return valor == null ? padrao : Boolean.parseBoolean(valor);
    }

    private static String descricaoOuPadrao(String descricao, int max) {
        String texto = descricao == null ? "NÃO INFORMADO" : descricao;
        return texto.length() > max ? texto.substring(0, max) : texto;
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
        return criar(request, FONTE_MANUAL);
    }

    private FornecedorResponse criar(FornecedorRequest request, String fonteEnderecos) {
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
        substituirEnderecos(fornecedor, request.enderecos(), fonteEnderecos);

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
        substituirEnderecos(fornecedor, requests, FONTE_MANUAL);
    }

    private void substituirEnderecos(
            Fornecedor fornecedor,
            List<EnderecoRequest> requests,
            String fonte) {
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
            endereco.setFonteDados(fonte);
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
