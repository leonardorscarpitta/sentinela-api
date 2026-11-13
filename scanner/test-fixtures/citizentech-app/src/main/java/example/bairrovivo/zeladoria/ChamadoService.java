package example.bairrovivo.zeladoria;

import org.springframework.stereotype.Service;

@Service
public class ChamadoService {

    public void abrir(Chamado chamado) {
        String contato = chamado.getTelefoneContato();
        if (contato == null || !contato.matches("\\(\\d{2}\\) 9\\d{4}-\\d{4}")) {
            // LEAK (telefone -> exception message): the message reaches logs and error responses.
            throw new IllegalArgumentException("Telefone de contato inválido: " + contato);
        }
    }
}
