package br.edu.ifpb.curadoria.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fornecedor")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 14, unique = true)
    private String cnpj;

    @Column(length = 14)
    private String cpf;

    @Column(nullable = false, length = 250)
    private String razaoSocial;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(nullable = false)
    private Boolean habilitadoLicitar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cnae_codigo")
    private Cnae cnae;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natureza_juridica_codigo")
    private NaturezaJuridica naturezaJuridica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porte_empresa_codigo")
    private PorteEmpresa porteEmpresa;

    @OneToMany(
            mappedBy = "fornecedor",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Endereco> enderecos = new ArrayList<>();

    protected Fornecedor() {
    }

    public Fornecedor(
            String cnpj,
            String cpf,
            String razaoSocial,
            Boolean ativo,
            Boolean habilitadoLicitar) {
        this.cnpj = cnpj;
        this.cpf = cpf;
        this.razaoSocial = razaoSocial;
        this.ativo = ativo;
        this.habilitadoLicitar = habilitadoLicitar;
    }

    public void adicionarEndereco(Endereco endereco) {
        enderecos.add(endereco);
        endereco.setFornecedor(this);
    }

    public void removerEndereco(Endereco endereco) {
        enderecos.remove(endereco);
        endereco.setFornecedor(null);
    }

    public Long getId() {
        return id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getHabilitadoLicitar() {
        return habilitadoLicitar;
    }

    public void setHabilitadoLicitar(Boolean habilitadoLicitar) {
        this.habilitadoLicitar = habilitadoLicitar;
    }

    public Cnae getCnae() {
        return cnae;
    }

    public void setCnae(Cnae cnae) {
        this.cnae = cnae;
    }

    public NaturezaJuridica getNaturezaJuridica() {
        return naturezaJuridica;
    }

    public void setNaturezaJuridica(
            NaturezaJuridica naturezaJuridica) {
        this.naturezaJuridica = naturezaJuridica;
    }

    public PorteEmpresa getPorteEmpresa() {
        return porteEmpresa;
    }

    public void setPorteEmpresa(PorteEmpresa porteEmpresa) {
        this.porteEmpresa = porteEmpresa;
    }

    public List<Endereco> getEnderecos() {
        return enderecos;
    }
}