package br.edu.ifpb.curadoria.entity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "municipio",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_municipio_nome_uf",
                        columnNames = {"nome", "uf"}
                )
        }
)
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 2)
    private String uf;

    protected Municipio() {
    }

    public Municipio(String nome, String uf) {
        this.nome = nome;
        this.uf = uf;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}