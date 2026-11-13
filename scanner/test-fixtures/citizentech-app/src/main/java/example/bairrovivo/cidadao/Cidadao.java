package example.bairrovivo.cidadao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Cidadao {

    @Id
    @GeneratedValue
    private Long id;

    private String nome;

    private String cpf;

    private String email;

    private String telefone;

    // Stored as plain text and serialized with the entity: intentionally insecure test input.
    private String senha;

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getSenha() {
        return senha;
    }
}
