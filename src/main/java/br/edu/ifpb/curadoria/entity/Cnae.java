package br.edu.ifpb.curadoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cnae")
public class Cnae {

    @Id
    @Column(name = "codigo")
    private Integer codigo;

    @Column(nullable = false, length = 300)
    private String descricao;

    protected Cnae() {
    }

    public Cnae(Integer codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}