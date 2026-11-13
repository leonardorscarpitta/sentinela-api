package example.bairrovivo.zeladoria;

/** A neighbourhood maintenance request (buraco, iluminação, entulho...). */
public class Chamado {

    private String categoria;

    private String descricao;

    private String endereco;

    private String telefoneContato;

    public String getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getTelefoneContato() {
        return telefoneContato;
    }
}
